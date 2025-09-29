#!/bin/bash

# URL Scanner - One-Command Setup for Code Reviewers
# This script sets up the entire application with secure secrets and sample data

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo
    echo "=================================="
    echo "$1"
    echo "=================================="
    echo
}

# Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Generate random string
generate_random() {
    if command_exists openssl; then
        openssl rand -base64 $1 | tr -d "=+/" | cut -c1-$1
    else
        # Fallback for systems without openssl
        cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w $1 | head -n 1
    fi
}

# Main setup function
main() {
    print_header "🚀 URL Scanner - Automated Setup"
    
    # Step 1: Check prerequisites
    print_status "Checking prerequisites..."
    
    if ! command_exists docker; then
        print_error "Docker is not installed. Please install Docker first."
        print_status "Visit: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    if ! command_exists docker-compose; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        print_status "Visit: https://docs.docker.com/compose/install/"
        exit 1
    fi
    
    # Check if Docker is running
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    
    print_success "All prerequisites met!"
    
    # Step 2: Clean up any existing containers
    print_status "Cleaning up any existing containers..."
    docker-compose down -v --remove-orphans 2>/dev/null || true
    print_success "Cleanup completed!"
    
    # Step 3: Generate secure secrets
    print_status "Generating secure secrets..."
    
    DB_PASSWORD=$(generate_random 32)
    JWT_SECRET=$(generate_random 64)
    
    # Create .env file for Docker Compose
    cat > .env << EOF
# Auto-generated secrets for code review
# Generated on: $(date)

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=urlscanner
DB_USER=urlscanner
DB_PASSWORD=$DB_PASSWORD

# Application Configuration
SERVER_PORT=8080

# JWT Configuration
JWT_SECRET=$JWT_SECRET
JWT_EXPIRATION=86400000

# URL Scan API Configuration (placeholder - will be implemented)
URLSCAN_API_KEY=demo-api-key-for-review
URLSCAN_API_URL=https://urlscan.io/api/v1

# Development settings
SPRING_PROFILES_ACTIVE=demo
CACHE_TTL_MINUTES=60
EOF
    
    print_success "Secure secrets generated and saved to .env"
    
    # Step 4: Build and start services
    print_status "Building and starting services..."
    print_warning "This may take a few minutes on first run..."
    
    docker-compose up --build -d
    
    # Step 5: Wait for services to be ready
    print_status "Waiting for services to start..."
    
    # Wait for database
    print_status "Waiting for database..."
    sleep 10
    
    # Wait for application
    print_status "Waiting for application..."
    max_attempts=30
    attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
            break
        fi
        attempt=$((attempt + 1))
        printf "."
        sleep 2
    done
    echo
    
    if [ $attempt -eq $max_attempts ]; then
        print_error "Application failed to start within expected time"
        print_status "Check logs with: docker-compose logs"
        exit 1
    fi
    
    print_success "Application is ready!"
    
    # Step 6: Create sample user and data
    print_status "Creating sample user and test data..."
    
    # Register a sample user
    SAMPLE_USER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/register \
        -H "Content-Type: application/json" \
        -d '{
            "email": "reviewer@example.com",
            "password": "reviewer123",
            "firstName": "Code",
            "lastName": "Reviewer"
        }' || echo "failed")
    
    if [[ $SAMPLE_USER_RESPONSE == *"token"* ]]; then
        print_success "Sample user created: reviewer@example.com / reviewer123"
        
        # Extract token for creating sample scans
        TOKEN=$(echo $SAMPLE_USER_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        
        # Create sample scans
        print_status "Creating sample URL scans..."
        
        curl -s -X POST http://localhost:8080/api/scans \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -d '{"url": "https://google.com"}' >/dev/null
            
        curl -s -X POST http://localhost:8080/api/scans \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -d '{"url": "https://github.com"}' >/dev/null
            
        curl -s -X POST http://localhost:8080/api/scans \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -d '{"url": "https://stackoverflow.com"}' >/dev/null
        
        print_success "Sample scan data created!"
    else
        print_warning "Could not create sample user (application might still be starting)"
    fi
    
    # Step 7: Display success message and instructions
    print_header "🎉 Setup Complete!"
    
    echo -e "${GREEN}Application is now running at: ${BLUE}http://localhost:8080${NC}"
    echo
    echo -e "${YELLOW}Sample User Credentials:${NC}"
    echo -e "  Email: ${BLUE}reviewer@example.com${NC}"
    echo -e "  Password: ${BLUE}reviewer123${NC}"
    echo
    echo -e "${YELLOW}Quick Test Commands:${NC}"
    echo
    echo -e "${BLUE}# 1. Health Check${NC}"
    echo "curl http://localhost:8080/actuator/health"
    echo
    echo -e "${BLUE}# 2. Login${NC}"
    echo 'curl -X POST http://localhost:8080/api/auth/login \'
    echo '  -H "Content-Type: application/json" \'
    echo '  -d '"'"'{"email":"reviewer@example.com","password":"reviewer123"}'"'"
    echo
    echo -e "${BLUE}# 3. List Scans (use token from login)${NC}"
    echo 'curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/scans'
    echo
    echo -e "${YELLOW}Management Commands:${NC}"
    echo -e "  View logs: ${BLUE}docker-compose logs -f${NC}"
    echo -e "  Stop services: ${BLUE}docker-compose down${NC}"
    echo -e "  Restart: ${BLUE}docker-compose restart${NC}"
    echo
    echo -e "${GREEN}Happy Code Reviewing! 🚀${NC}"
    echo
}

# Trap to cleanup on exit
trap 'echo -e "\n${YELLOW}Setup interrupted. Run ${BLUE}docker-compose down${NC} to clean up."' INT TERM

# Run main function
main "$@"