# URL Scanner Application

A self-service Spring Boot application for scanning URLs for security issues using the urlscan.io API. The system provides JWT-based authentication, asynchronous scan processing, result caching, and a comprehensive REST API for developers to manage their URL scans.

---

## 🚀 How to Run and Test with Docker

This guide provides the essential steps to get the application running locally using Docker and test the end-to-end URL scanning process.

### Prerequisites

- Docker and Docker Compose

### Step 1: Get Your urlscan.io API Key

This application requires an API key from [urlscan.io](https://urlscan.io/).

1.  Create a free account on [urlscan.io](https://urlscan.io/).
2.  Go to your **Dashboard** -> **API** page.
3.  Copy your API key.

### Step 2: Create Your Environment File

Create a `.env` file by copying the provided template. This file will store your API key and other configuration secrets.

```bash
cp .env.example .env
```

Now, open the `.env` file and paste your API key into the `URLSCAN_API_KEY` field.

```dotenv
# .env

# ... other settings

# Get your API key from https://urlscan.io/
URLSCAN_API_KEY=YOUR_API_KEY_HERE
```

### Step 3: Run the Application with Docker Compose

With your `.env` file configured, start the entire application stack with a single command:

```bash
docker-compose up --build
```

This command will:
- Build the Spring Boot application Docker image.
- Start a PostgreSQL database container.
- Start the URL Scanner application container.

The application will be available at `http://localhost:8080`.

### Step 4: Test the End-to-End Scanning Flow

Use the following `curl` commands in your terminal to interact with the API.

**1. Register a New User**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "developer@example.com",
    "password": "securepass123",
    "firstName": "Jane",
    "lastName": "Developer"
  }'
```

**2. Log In and Get Your JWT Token**

```bash
# The response will contain your JWT token. Copy it for the next steps.
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "developer@example.com", "password": "securepass123"}'
```

**3. Submit a URL for Scanning**

Replace `$TOKEN` with the token you received.

```bash
# Store your token in a variable
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# Submit a URL (e.g., example.com)
curl -X POST http://localhost:8080/api/scans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"url": "https://example.com"}'
```

The API will immediately respond with the initial scan record. Note the `id` and the status `SUBMITTED`.

**4. Check the Scan Status (Polling)**

Wait a few seconds for the background worker to pick up the job, then check the scan's status using its ID. You should see the status change to `PROCESSING`.

```bash
# Check the status of scan with ID 1
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/scans/1
```

**5. Get the Final Result**

Wait another 15-30 seconds for the scan to complete. Poll the same endpoint again. The status will change to `DONE`, and the `result` field will be populated with the JSON report from `urlscan.io`.

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/scans/1
```

Congratulations! You have successfully tested the entire asynchronous scanning workflow.

---

## API Endpoints

All scan endpoints require authentication (`Authorization: Bearer <your-jwt-token>`).

- `POST /api/scans`: Submit a URL for scanning.
- `GET /api/scans`: List all scans submitted by the user.
- `GET /api/scans/{id}`: Return details and results of a specific scan.
- `DELETE /api/scans/{id}`: Delete a specific scan request and its results.

**Available scan statuses**: `SUBMITTED`, `PROCESSING`, `DONE`, `FAILED`

## Architecture and Design

The system is designed as a classic asynchronous worker architecture to ensure the API remains responsive and resilient.

1.  **API Layer (`UrlScanController`)**: A thin, non-blocking layer for authentication, validation, and immediately persisting scan requests with a `SUBMITTED` status.
2.  **Persistence Layer (PostgreSQL)**: Acts as a reliable queue, decoupling the API from the background workers. This guarantees no requests are lost.
3.  **Worker Layer (`UrlScanWorker`)**: A stateless background process with two scheduled tasks:
    *   **Submission Worker**: Polls for `SUBMITTED` scans and sends them to `urlscan.io`.
    *   **Result Worker**: Polls for `PROCESSING` scans and fetches their results.
4.  **External Service Client (`UrlScanIoClient`)**: An encapsulated client that handles all communication with the `urlscan.io` API, including rate-limit handling.

### Fault Tolerance and Rate Limiting

- **Resilience**: Because the workers are stateless and the state is stored in the database, the system is highly fault-tolerant. If a worker dies, a new one will start and pick up the work exactly where the last one left off.
- **Rate Limiting**: The `UrlScanIoClient` includes a simple retry mechanism. If it receives a `429 Too Many Requests` error, it will pause and retry, preventing the service from being blocked by the external API.
- **User Experience**: The API feels fast because it responds immediately. The user can poll for status updates without having to wait for the full scan to complete, which is a key principle of good asynchronous design.

## Development

### Running Tests

```bash
mvn test
```

### Stopping Services

```bash
# Stop Docker Compose services
docker-compose down

# Remove volumes (this will delete all database data)
docker-compose down -v
```
