#!/bin/bash

# URL Scanner CLI Tool
# A simple command-line interface for the URL Scanner API

# Configuration
API_BASE_URL="${URLSCANNER_API_URL:-http://localhost:8080}"
TOKEN_FILE="$HOME/.urlscanner_token"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
print_error() {
    echo -e "${RED}Error: $1${NC}" >&2
}

print_success() {
    echo -e "${GREEN}$1${NC}"
}

print_info() {
    echo -e "${BLUE}$1${NC}"
}

print_warning() {
    echo -e "${YELLOW}$1${NC}"
}

# Check if jq is installed
check_dependencies() {
    if ! command -v jq &> /dev/null; then
        print_error "jq is required but not installed. Install it with:"
        echo "  macOS: brew install jq"
        echo "  Ubuntu/Debian: sudo apt-get install jq"
        echo "  CentOS/RHEL: sudo yum install jq"
        exit 1
    fi
}

# Save token to file
save_token() {
    echo "$1" > "$TOKEN_FILE"
    chmod 600 "$TOKEN_FILE"
}

# Load token from file
load_token() {
    if [ -f "$TOKEN_FILE" ]; then
        cat "$TOKEN_FILE"
    else
        echo ""
    fi
}

# Register a new user
register() {
    if [ $# -lt 2 ]; then
        print_error "Usage: urlscanner register <email> <password> [firstName] [lastName]"
        exit 1
    fi

    local email="$1"
    local password="$2"
    local firstName="${3:-User}"
    local lastName="${4:-Scanner}"

    print_info "Registering user: $email..."

    response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE_URL/api/auth/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"$email\",
            \"password\": \"$password\",
            \"firstName\": \"$firstName\",
            \"lastName\": \"$lastName\"
        }")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
        print_success "✓ Registration successful!"
        echo "$body" | jq '.'
    else
        print_error "Registration failed (HTTP $http_code)"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
        exit 1
    fi
}

# Login and save token
login() {
    if [ $# -lt 2 ]; then
        print_error "Usage: urlscanner login <email> <password>"
        exit 1
    fi

    local email="$1"
    local password="$2"

    print_info "Logging in as: $email..."

    response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"$email\",
            \"password\": \"$password\"
        }")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq 200 ]; then
        token=$(echo "$body" | jq -r '.token')
        if [ -n "$token" ] && [ "$token" != "null" ]; then
            save_token "$token"
            print_success "✓ Login successful! Token saved."
            echo "$body" | jq '.'
        else
            print_error "Failed to extract token from response"
            exit 1
        fi
    else
        print_error "Login failed (HTTP $http_code)"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
        exit 1
    fi
}

# Logout (remove token)
logout() {
    if [ -f "$TOKEN_FILE" ]; then
        rm "$TOKEN_FILE"
        print_success "✓ Logged out successfully. Token removed."
    else
        print_info "No active session found."
    fi
}

# Submit a URL for scanning
scan() {
    if [ $# -lt 1 ]; then
        print_error "Usage: urlscanner scan <url>"
        exit 1
    fi

    local url="$1"
    local token=$(load_token)

    if [ -z "$token" ]; then
        print_error "Not logged in. Please run: urlscanner login <email> <password>"
        exit 1
    fi

    print_info "Submitting scan for: $url..."

    response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE_URL/api/scans" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $token" \
        -d "{\"url\": \"$url\"}")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
        scan_id=$(echo "$body" | jq -r '.id')
        print_success "✓ Scan submitted successfully! (ID: $scan_id)"
        echo "$body" | jq '.'
        print_info "\nCheck status with: urlscanner status $scan_id"
    else
        print_error "Scan submission failed (HTTP $http_code)"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
        exit 1
    fi
}

# Get scan status
status() {
    if [ $# -lt 1 ]; then
        print_error "Usage: urlscanner status <scan_id>"
        exit 1
    fi

    local scan_id="$1"
    local token=$(load_token)

    if [ -z "$token" ]; then
        print_error "Not logged in. Please run: urlscanner login <email> <password>"
        exit 1
    fi

    print_info "Fetching scan status for ID: $scan_id..."

    response=$(curl -s -w "\n%{http_code}" -X GET "$API_BASE_URL/api/scans/$scan_id" \
        -H "Authorization: Bearer $token")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq 200 ]; then
        scan_status=$(echo "$body" | jq -r '.scanStatus')

        case "$scan_status" in
            "SUBMITTED")
                print_warning "⏳ Status: SUBMITTED - Waiting in queue..."
                ;;
            "PROCESSING")
                print_warning "⚙️  Status: PROCESSING - Scan in progress..."
                ;;
            "DONE")
                print_success "✓ Status: DONE - Scan completed!"
                ;;
            "FAILED")
                print_error "✗ Status: FAILED - Scan failed"
                ;;
        esac

        echo "$body" | jq '.'
    else
        print_error "Failed to fetch scan status (HTTP $http_code)"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
        exit 1
    fi
}

# List all scans
list() {
    local token=$(load_token)

    if [ -z "$token" ]; then
        print_error "Not logged in. Please run: urlscanner login <email> <password>"
        exit 1
    fi

    print_info "Fetching all your scans..."

    response=$(curl -s -w "\n%{http_code}" -X GET "$API_BASE_URL/api/scans" \
        -H "Authorization: Bearer $token")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq 200 ]; then
        count=$(echo "$body" | jq 'length')
        print_success "✓ Found $count scan(s)"
        echo "$body" | jq '.'
    else
        print_error "Failed to fetch scans (HTTP $http_code)"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
        exit 1
    fi
}

# Delete a scan
delete() {
    if [ $# -lt 1 ]; then
        print_error "Usage: urlscanner delete <scan_id>"
        exit 1
    fi

    local scan_id="$1"
    local token=$(load_token)

    if [ -z "$token" ]; then
        print_error "Not logged in. Please run: urlscanner login <email> <password>"
        exit 1
    fi

    print_info "Deleting scan ID: $scan_id..."

    response=$(curl -s -w "\n%{http_code}" -X DELETE "$API_BASE_URL/api/scans/$scan_id" \
        -H "Authorization: Bearer $token")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 204 ]; then
        print_success "✓ Scan deleted successfully!"
    else
        print_error "Failed to delete scan (HTTP $http_code)"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
        exit 1
    fi
}

# Show current session info
whoami() {
    local token=$(load_token)

    if [ -z "$token" ]; then
        print_info "Not logged in."
        exit 0
    fi

    # Decode JWT token (without verification)
    payload=$(echo "$token" | cut -d'.' -f2)
    # Add padding if needed
    padding=$((4 - ${#payload} % 4))
    [ $padding -eq 4 ] && padding=0
    padded_payload="$payload$(printf '%*s' $padding | tr ' ' '=')"

    decoded=$(echo "$padded_payload" | base64 -d 2>/dev/null | jq '.' 2>/dev/null)

    if [ $? -eq 0 ]; then
        print_success "✓ Logged in as:"
        echo "$decoded" | jq '{email: .sub, issued: .iat, expires: .exp}'
    else
        print_info "Token exists but cannot be decoded."
    fi
}

# Watch scan status (poll until done)
watch_scan() {
    if [ $# -lt 1 ]; then
        print_error "Usage: urlscanner watch <scan_id>"
        exit 1
    fi

    local scan_id="$1"
    local token=$(load_token)

    if [ -z "$token" ]; then
        print_error "Not logged in. Please run: urlscanner login <email> <password>"
        exit 1
    fi

    print_info "Watching scan ID: $scan_id (press Ctrl+C to stop)..."
    echo ""

    while true; do
        response=$(curl -s -w "\n%{http_code}" -X GET "$API_BASE_URL/api/scans/$scan_id" \
            -H "Authorization: Bearer $token")

        http_code=$(echo "$response" | tail -n1)
        body=$(echo "$response" | sed '$d')

        if [ "$http_code" -eq 200 ]; then
            scan_status=$(echo "$body" | jq -r '.scanStatus')
            url=$(echo "$body" | jq -r '.url')

            case "$scan_status" in
                "SUBMITTED")
                    echo -ne "\r⏳ $url - Status: SUBMITTED (waiting in queue...)    "
                    ;;
                "PROCESSING")
                    echo -ne "\r⚙️  $url - Status: PROCESSING (scanning...)         "
                    ;;
                "DONE")
                    echo -e "\r✓ $url - Status: DONE (completed!)                   "
                    echo ""
                    echo "$body" | jq '.'
                    break
                    ;;
                "FAILED")
                    echo -e "\r✗ $url - Status: FAILED                              "
                    echo ""
                    failure_reason=$(echo "$body" | jq -r '.failureReason // "Unknown"')
                    print_error "Failure reason: $failure_reason"
                    break
                    ;;
            esac
        else
            print_error "\nFailed to fetch scan status"
            break
        fi

        sleep 2
    done
}

# Display help
show_help() {
    cat << EOF
URL Scanner CLI - Command-line tool for URL security scanning

Usage: urlscanner <command> [options]

Authentication:
  register <email> <password> [firstName] [lastName]
                        Register a new user account
  login <email> <password>
                        Login and save authentication token
  logout                Remove saved authentication token
  whoami                Show current user information

Scanning:
  scan <url>            Submit a URL for security scanning
  status <scan_id>      Check the status of a scan
  watch <scan_id>       Watch a scan until it completes
  list                  List all your scans
  delete <scan_id>      Delete a scan

Configuration:
  Set custom API URL with environment variable:
    export URLSCANNER_API_URL=http://your-server:8080

Examples:
  # Register and login
  urlscanner register user@example.com mypassword123 John Doe
  urlscanner login user@example.com mypassword123

  # Scan URLs
  urlscanner scan https://example.com
  urlscanner scan https://github.com

  # Check scan status
  urlscanner status 1
  urlscanner watch 1

  # List and manage scans
  urlscanner list
  urlscanner delete 5

  # Check current session
  urlscanner whoami
  urlscanner logout

For more information, visit: https://github.com/your-repo/urlscanner
EOF
}

# Main command dispatcher
main() {
    check_dependencies

    if [ $# -eq 0 ]; then
        show_help
        exit 0
    fi

    case "$1" in
        register)
            shift
            register "$@"
            ;;
        login)
            shift
            login "$@"
            ;;
        logout)
            logout
            ;;
        scan)
            shift
            scan "$@"
            ;;
        status)
            shift
            status "$@"
            ;;
        watch)
            shift
            watch_scan "$@"
            ;;
        list)
            list
            ;;
        delete)
            shift
            delete "$@"
            ;;
        whoami)
            whoami
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "Unknown command: $1"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

main "$@"