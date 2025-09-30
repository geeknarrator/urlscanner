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

### Step 4: Explore the Grafana Dashboard

Once the services are running, you can immediately view the application's metrics.

1.  Open your browser and navigate to **http://localhost:3000**.
2.  Log in with the default credentials:
    -   Username: `admin`
    -   Password: `admin`
    (You can skip the password change prompt).
3.  Navigate to **Dashboards** from the left-hand menu.
4.  Click on the **URL Scanner Overview** dashboard.

You will see a pre-built dashboard visualizing key metrics like pending scans, scan throughput, cache hits, and failure rates.

### Step 5: Test the End-to-End Scanning Flow

As you use the API, you will see the dashboard update in real-time. Use the following `curl` commands to generate some metrics.

**1. Register and Log In**

```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"email": "developer@example.com", "password": "securepass123", "firstName": "Jane", "lastName": "Developer"}'

# Log in and get your token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "developer@example.com", "password": "securepass123"}' | jq -r .token)

echo "Your token is: $TOKEN"
```

**2. Submit URLs and Watch the Dashboard**

Submit a few URLs for scanning. As you do, watch the "Scan Throughput" and "Pending Scans" panels on your Grafana dashboard.

```bash
curl -X POST http://localhost:8080/api/scans -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{"url": "https://google.com"}'
curl -X POST http://localhost:8080/api/scans -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{"url": "https://github.com"}'
```

---

## Observability: The Monitoring Stack

The project includes a complete, pre-configured monitoring stack to provide insight into the application's performance and health.

- **Metrics Exposure**: The Spring Boot application uses **Micrometer** to expose detailed metrics (including custom ones like `scans.completed` and `scans.failed`) at the `/actuator/prometheus` endpoint.
- **Metrics Collection**: A **Prometheus** service is configured to automatically scrape these metrics every 15 seconds and store them in a time-series database.
- **Metrics Visualization**: A **Grafana** service is pre-configured with Prometheus as a data source and includes a provisioned **URL Scanner Overview** dashboard to visualize the most important application metrics out of the box.

## API Endpoints

All scan endpoints require authentication (`Authorization: Bearer <your-jwt-token>`).

- `POST /api/scans`: Submit a URL for scanning.
- `GET /api/scans`: List all scans submitted by the user.
- `GET /api/scans/{id}`: Return details and results of a specific scan.
- `DELETE /api/scans/{id}`: Delete a specific scan request and its results.

## Development

### Running Tests

```bash
mvn test
```

### Stopping Services

```bash
# Stop all Docker Compose services
docker-compose down

# Stop services and remove all data volumes
docker-compose down -v
```
