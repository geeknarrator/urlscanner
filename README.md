# URL Scanner Application

A Spring Boot application for URL scanning with PostgreSQL database support, containerized with Docker and ready for Kubernetes deployment.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- (Optional) Kubernetes cluster for production deployment

## Quick Start - Docker Compose (Recommended)

The easiest way to run the application locally is using Docker Compose:

```bash
# Clone or navigate to the project directory
cd urlscanner

# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up --build -d
```

This will start:
- PostgreSQL database on port 5432
- URL Scanner application on port 8080

The application will be available at: http://localhost:8080

## Running Locally without Docker

### 1. Start PostgreSQL Database

First, ensure PostgreSQL is running locally with the following configuration:
- Database: `urlscanner`
- Username: `urlscanner`
- Password: `password`
- Port: 5432

You can create the database and user:
```sql
CREATE DATABASE urlscanner;
CREATE USER urlscanner WITH ENCRYPTED PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE urlscanner TO urlscanner;
```

Run the initialization script:
```bash
psql -h localhost -U urlscanner -d urlscanner -f init.sql
```

### 2. Build and Run the Application

```bash
# Build the application
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

Or using Maven directly:
```bash
mvn clean install
mvn spring-boot:run
```

The application will start on port 8080.

## API Endpoints

The application provides the following REST endpoints:

### Create a new URL scan
```bash
curl -X POST http://localhost:8080/api/scans \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com"}'
```

### Get all scans
```bash
curl http://localhost:8080/api/scans
```

### Get scan by ID
```bash
curl http://localhost:8080/api/scans/1
```

### Get scans by status
```bash
curl http://localhost:8080/api/scans/status/PENDING
```

Available statuses: `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`

## Health Check

The application includes Spring Boot Actuator for health monitoring:

```bash
curl http://localhost:8080/actuator/health
```

## Configuration

The application can be configured using environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | urlscanner | Database name |
| `DB_USER` | urlscanner | Database username |
| `DB_PASSWORD` | password | Database password |
| `SERVER_PORT` | 8080 | Application server port |

## Development

### Running Tests
```bash
./mvnw test
```

### Building Docker Image
```bash
docker build -t urlscanner:latest .
```

### Stopping Services
```bash
# Stop Docker Compose services
docker-compose down

# Remove volumes (this will delete database data)
docker-compose down -v
```

## Kubernetes Deployment

For production deployment on Kubernetes:

```bash
# Apply all Kubernetes manifests
kubectl apply -f k8s/

# Check deployment status
kubectl get pods -n urlscanner

# Get service URL (for LoadBalancer)
kubectl get svc -n urlscanner
```

To delete the deployment:
```bash
kubectl delete -f k8s/
```

## Project Structure

```
urlscanner/
├── src/main/java/com/geeknarrator/urlscanner/
│   ├── controller/          # REST controllers
│   ├── entity/             # JPA entities
│   ├── repository/         # Data repositories
│   └── UrlScannerApplication.java
├── src/main/resources/
│   └── application.yml     # Application configuration
├── k8s/                    # Kubernetes manifests
├── docker-compose.yml      # Docker Compose configuration
├── Dockerfile             # Docker build configuration
├── init.sql              # Database initialization
└── pom.xml               # Maven dependencies
```

## Troubleshooting

### Database Connection Issues
- Ensure PostgreSQL is running and accessible
- Check database credentials and connection string
- Verify the database and user exist

### Port Already in Use
- Change the `SERVER_PORT` environment variable
- Or stop the service using the port

### Docker Issues
- Ensure Docker daemon is running
- Check available disk space
- Try `docker-compose down` and `docker-compose up --build` again

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test locally using Docker Compose
5. Submit a pull request
