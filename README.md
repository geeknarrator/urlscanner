# URL Scanner Application

A self-service Spring Boot application for scanning URLs for security issues using the urlscan.io API. The system provides JWT-based authentication, asynchronous scan processing, result caching, and a comprehensive REST API for developers to manage their URL scans.

This project is fully containerized and includes a complete observability stack with Prometheus and Grafana for monitoring.

---

## 🚀 How to Run and Test with Docker

This guide provides the essential steps to get the application and its monitoring stack running locally using Docker.

### Prerequisites

- Docker and Docker Compose
- Java 17 or higher (for local development and testing)
- Maven 3.6+ (for building and running tests)

### Step 1: Get Your urlscan.io API Key

This application requires an API key from [urlscan.io](https://urlscan.io/).

1.  Create a free account on [urlscan.io](https://urlscan.io/).
2.  Go to your **Dashboard** -> User Profile -> Settings & API -> New **API** key page.
3.  Copy your API key.

### Step 2: Setup Configuration & Secrets (in the repository)

#### Create Your Environment File

1. **Copy the template**:
   ```bash
   cp .env.example .env
   ```

2. **Open the `.env` file** and configure these settings:

#### ✅ Required Configuration

**1. URLSCAN_API_KEY** - Paste your API key from Step 1
```dotenv
URLSCAN_API_KEY=your-actual-api-key-here
```

**2. JWT_SECRET** - Generate a secure signing key
```bash
# Generate a strong random key (copy the output)
openssl rand -base64 64
```
Then paste it in your `.env` (make sure it is in a single line):
```dotenv
JWT_SECRET=paste-the-generated-key-here
```

#### ⚙️ Optional Settings (Fine for Testing, Change for Production)

The `.env` file has these defaults:
- `DB_PASSWORD=password` ⚠️ Change for production
- `JWT_EXPIRATION=86400000` (24 hours)
- `WORKER_SUBMISSION_DELAY_MS=10000` (10 seconds)

**🔒 Important Security Notes**:
- ✅ Never commit your `.env` file to git (it's already in `.gitignore`)
- ✅ Use different secrets for development and production
- ✅ For production, generate strong random passwords: `openssl rand -base64 32`
- ✅ Rotate secrets regularly in production environments

### Step 3: Run the Entire Stack

With your `.env` file configured, start the entire application and monitoring stack with a single command:

```bash
docker-compose up --build
```

This command will start the following services:
- **URL Scanner App**: The main application, available at `http://localhost:8080`.
- **PostgreSQL**: The database for the application.
- **Prometheus**: The metrics collector, available at `http://localhost:9090`.
- **Grafana**: The visualization dashboard, available at `http://localhost:3000` under dashboards http://localhost:3000/d/urlscanner-overview/url-scanner-overview.

### Step 4: Test the API

#### Option 1: Using the CLI Tool (Easiest!)

A simple command-line tool is provided for easy interaction with the API.

**Quick Install:**
```bash
# Install the CLI (system-wide)
sudo ./install-cli.sh

# Or install for current user only
./install-cli.sh

# Install jq dependency (required for JSON parsing)
brew install jq  # macOS
# sudo apt-get install jq  # Ubuntu/Debian
# sudo yum install jq      # CentOS/RHEL
```

**Usage Examples:**

```bash
# Register a new user
urlscanner register demo@example.com securepass123 Demo User

# Login (saves token automatically)
urlscanner login demo@example.com securepass123

# Scan URLs
urlscanner scan https://example.com
urlscanner scan https://github.com

# Check scan status
urlscanner status 1

# Watch scan until completion (auto-refresh)
urlscanner watch 1

# List all your scans
urlscanner list

# Delete a scan
urlscanner delete 5

# Check who's logged in
urlscanner whoami

# Logout
urlscanner logout
```

**Features:**
- ✅ **Automatic token management** - Login once, use everywhere
- ✅ **Color-coded output** - Easy to read status messages
- ✅ **Watch mode** - Real-time scan progress monitoring
- ✅ **Smart error handling** - Clear error messages with suggestions
- ✅ **JWT decoding** - See your session info with `whoami`

---

#### Option 2: Using cURL (Manual)

**1. Register a new user:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "securepass123",
    "firstName": "Demo",
    "lastName": "User"
  }'
```

**2. Login to get your JWT token:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "securepass123"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "demo@example.com"
}
```

**💡 Tip**: Copy the `token` value from the response - you'll need it for the next steps!

**3. Submit a URL for scanning:**

Replace `YOUR_TOKEN_HERE` with the actual token from step 2:

```bash
# Set your token as a variable for easy reuse
export TOKEN="YOUR_TOKEN_HERE"

# Submit a URL scan
curl -X POST http://localhost:8080/api/scans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "url": "https://example.com"
  }'
```

**Response:**
```json
{
  "id": 1,
  "url": "https://example.com",
  "scanStatus": "SUBMITTED",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**4. Check scan status:**

Replace `SCAN_ID` with the `id` from the previous response:

```bash
curl http://localhost:8080/api/scans/1 \
  -H "Authorization: Bearer $TOKEN"
```

**5. Get all your scans:**
```bash
curl http://localhost:8080/api/scans \
  -H "Authorization: Bearer $TOKEN"
```

#### More Example URLs to Scan

```bash
# Scan multiple URLs
curl -X POST http://localhost:8080/api/scans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"url": "https://github.com"}'

curl -X POST http://localhost:8080/api/scans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"url": "https://stackoverflow.com"}'

curl -X POST http://localhost:8080/api/scans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"url": "https://reddit.com"}'
```

#### Using Swagger UI (Alternative)

If you prefer a graphical interface:

1.  Open your browser and navigate to **http://localhost:8080/swagger-ui/index.html**.
2.  You will see a complete, interactive documentation of all available endpoints.
3.  Click **Authorize** at the top, enter `Bearer YOUR_TOKEN_HERE`, and click **Authorize** again.
4.  Now you can test any endpoint directly from the browser!

### Step 5: View the Grafana Dashboard

1.  Open your browser and navigate to **http://localhost:3000**.
2.  Log in with the default credentials: `admin` / `admin`.
3.  Navigate to **Dashboards** and click on the **URL Scanner Overview** dashboard to see your application's metrics in real-time.

---

## Architecture and Design

The system is designed as a robust, multi-tenant, asynchronous worker platform.

1.  **API Layer**: A thin, non-blocking layer for authentication, validation, and immediately persisting scan requests.
2.  **Persistence Layer (PostgreSQL)**: Acts as a reliable queue, decoupling the API from the background workers.
3.  **Worker Layer (`UrlScanWorker`)**: A stateless background process that uses a sophisticated **two-phase fairness queueing** model:
    *   **Fairness Pass**: A round-robin process that gives every user with pending jobs a fair share of the processing resources in each run.
    *   **Efficiency Pass**: A bulk-fetch process that utilizes the worker's remaining capacity to maximize throughput.
    *   **Concurrency Safety**: The worker uses pessimistic database locks (`SELECT ... FOR UPDATE SKIP LOCKED`) to ensure that even when scaled to multiple instances, no two workers will ever process the same job.
4.  **External Service Client (`UrlScanIoClient`)**: An encapsulated client that handles all communication with the `urlscan.io` API, including rate-limit handling with configurable exponential backoff.

## Key Features

- **Authentication**: Secure JWT-based authentication with user registration and login.
- **User Isolation**: All API operations are strictly scoped to the authenticated user.
- **Scalable & Fair Worker Queue**: The background worker is designed to be both horizontally scalable and fair to all users.
- **Result Caching**: A two-layer cache minimizes redundant API calls and provides instant results for frequently scanned URLs.
- **Full Observability**: The application is fully instrumented with custom metrics, which are collected by a pre-configured Prometheus and Grafana stack.
- **Interactive API Documentation**: A built-in Swagger UI provides comprehensive, interactive documentation for the entire REST API.
- **Fully Configurable**: All key operational parameters (worker schedules, batch sizes, API client retries, etc.) are exposed as configurable properties.

## Configuration Reference

All configuration is managed through environment variables defined in the `.env` file. Below is a complete reference:

### Required Settings

| Variable | Description | Example | Required |
|----------|-------------|---------|----------|
| `URLSCAN_API_KEY` | Your urlscan.io API key | `abc123...` | ✅ Yes |
| `JWT_SECRET` | JWT signing secret (256+ bits) | Generate with `openssl rand -base64 64` | ✅ Production |

### Database Settings

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `db` (Docker service name) |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `urlscanner` |
| `DB_USER` | Database username | `urlscanner` |
| `DB_PASSWORD` | Database password | `password` ⚠️ Change for production |

### Application Settings

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Application HTTP port | `8080` |
| `JWT_EXPIRATION` | Token expiration (milliseconds) | `86400000` (24 hours) |

### Worker Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `WORKER_SUBMISSION_DELAY_MS` | Delay between submission worker runs | `10000` (10 seconds) |
| `WORKER_SUBMISSION_BATCH_SIZE` | Max scans per submission run | `100` |
| `WORKER_RESULT_DELAY_MS` | Delay between result worker runs | `15000` (15 seconds) |
| `WORKER_RESULT_BATCH_SIZE` | Max results to fetch per run | `100` |
| `WORKER_FAIRNESS_PER_USER_BATCH_SIZE` | Scans per user in fairness pass | `5` |

### Cache & Client Settings

| Variable | Description | Default |
|----------|-------------|---------|
| `URLSCAN_CACHE_TTL_HOURS` | How long to cache scan results | `24` (hours) |
| `URLSCAN_CLIENT_MAX_RETRIES` | Max retries for failed API calls | `3` |
| `URLSCAN_CLIENT_RETRY_DELAY_MS` | Initial retry delay (exponential backoff) | `5000` (5 seconds) |

## Development

### Running Tests

The project includes comprehensive unit and integration tests. Integration tests use Testcontainers, which requires Docker to be running.

```bash
# Ensure Docker is running
mvn clean test
```

The test suite includes:
- **Unit Tests**: Controller and service layer tests with mocked dependencies
- **Integration Tests**: Full application context tests with PostgreSQL container
- **Repository Tests**: JPA repository tests with real database interactions

### Stopping Services

```bash
# Stop all Docker Compose services
docker-compose down

# Stop services and remove all data volumes
docker-compose down -v
```

---

## 🔐 Security Best Practices

### DO NOT:
- ❌ Commit `.env` files to git
- ❌ Use default secrets in production (like `password`)
- ❌ Share secrets in plain text (Slack, email, etc.)
- ❌ Use short JWT secrets (minimum 64 characters for production)
- ❌ Hardcode secrets in source code

### DO:
- ✅ Use strong, randomly generated secrets in production
- ✅ Rotate secrets regularly (every 90 days minimum)
- ✅ Use different secrets per environment (dev, staging, prod)
- ✅ Store production secrets securely

### Generating Strong Secrets

```bash
# Database password (32 characters)
openssl rand -base64 32

# JWT signing secret (64+ characters for HS256)
openssl rand -base64 64
```

---

## 🆘 Troubleshooting

### Application won't start

**Check your `.env` file exists**:
```bash
ls -la .env
```

**Verify required variables are set**:
```bash
grep URLSCAN_API_KEY .env
grep JWT_SECRET .env
```

### Authentication errors (401 Unauthorized)

**Check JWT secret is set correctly**:
- JWT secret must be the same value the application started with
- Restarting with a different JWT secret invalidates all existing tokens
- Solution: Re-login to get a new token

### Database connection errors

**Verify Docker services are running**:
```bash
docker-compose ps
```

**Check database is healthy**:
```bash
docker-compose logs db
```

### Can't access Swagger UI

**Verify the application is running**:
```bash
curl http://localhost:8080/actuator/health
```

**Try the correct Swagger URL**: `http://localhost:8080/swagger-ui/index.html`

### Tests failing

**Ensure Docker is running** (required for Testcontainers):
```bash
docker ps
```

**Run tests with more verbose output**:
```bash
mvn test -X
```
