# URL Scanner Application

A self-service Spring Boot application for scanning URLs for security issues using the urlscan.io API. The system provides JWT-based authentication, asynchronous scan processing, result caching, and a comprehensive REST API for developers to manage their URL scans.

This project is fully containerized and includes a complete observability stack with Prometheus and Grafana for monitoring.

---

## 🚀 How to Run and Test with Docker

This guide provides the essential steps to get the application and its monitoring stack running locally using Docker.

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

### Step 3: Run the Entire Stack

With your `.env` file configured, start the entire application and monitoring stack with a single command:

```bash
docker-compose up --build
```

This command will start the following services:
- **URL Scanner App**: The main application, available at `http://localhost:8080`.
- **PostgreSQL**: The database for the application.
- **Prometheus**: The metrics collector, available at `http://localhost:9090`.
- **Grafana**: The visualization dashboard, available at `http://localhost:3000`.

### Step 4: Explore the Interactive API Documentation

Once the application is running, you can explore and interact with the API using the built-in Swagger UI.

1.  Open your browser and navigate to **http://localhost:8080/swagger-ui.html**.
2.  You will see a complete, interactive documentation of all available endpoints.
3.  To test the secured endpoints, first use the `/api/auth/login` endpoint to get a JWT token. Then, click the **Authorize** button at the top of the page and paste your token in the format `Bearer <your-token>`.

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

## Development

### Running Tests

```bash
mvn clean test
```

### Stopping Services

```bash
# Stop all Docker Compose services
docker-compose down

# Stop services and remove all data volumes
docker-compose down -v
```
