# URL Scanner Application

A self-service Spring Boot application for scanning URLs for security issues using urlscan.io API. The system provides JWT-based authentication, asynchronous scan processing, result caching, and comprehensive REST APIs for developers to manage their URL scans.

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

## Authentication

The application uses JWT-based authentication. All API endpoints (except authentication and health check) require a valid JWT token.

### User Registration
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### User Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

Both endpoints return a JWT token that must be included in subsequent requests:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

## API Endpoints

All scan endpoints require authentication. Include the JWT token in the Authorization header:
```bash
Authorization: Bearer <your-jwt-token>
```

**Important**: All scan operations are user-scoped. Users can only view, create, and delete their own scans. The system automatically filters data based on the authenticated user's ID from the JWT token.

### Submit URL for Scanning
```bash
curl -X POST http://localhost:8080/api/scans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{"url": "https://example.com"}'
```

**URL Validation**: URLs must start with `http://` or `https://`

Response:
```json
{
  "id": 1,
  "url": "https://example.com",
  "status": "SUBMITTED",
  "userId": 1,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00",
  "result": null,
  "externalScanId": null
}
```

### List All User Scans
Returns only scans belonging to the authenticated user, ordered by creation date (newest first):
```bash
curl http://localhost:8080/api/scans \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Get Specific Scan Details
Returns scan details only if the scan belongs to the authenticated user:
```bash
curl http://localhost:8080/api/scans/1 \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Delete a Scan
Deletes a scan only if it belongs to the authenticated user:
```bash
curl -X DELETE http://localhost:8080/api/scans/1 \
  -H "Authorization: Bearer <your-jwt-token>"
```

Returns `204 No Content` on success, `404 Not Found` if scan doesn't exist or doesn't belong to user.

**Available scan statuses**: `SUBMITTED`, `PROCESSING`, `DONE`, `FAILED`

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
| `JWT_SECRET` | mySecretKey | JWT signing secret (change in production) |
| `JWT_EXPIRATION` | 86400000 | JWT token expiration in milliseconds (24 hours) |

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
│   ├── controller/          # REST controllers (Auth, Scans)
│   ├── entity/             # JPA entities (User, UrlScan)
│   ├── repository/         # Data repositories
│   ├── security/           # JWT utilities and security config
│   ├── service/            # Business logic services
│   └── UrlScannerApplication.java
├── src/main/resources/
│   └── application.yml     # Application configuration
├── k8s/                    # Kubernetes manifests
├── docker-compose.yml      # Docker Compose configuration
├── Dockerfile             # Docker build configuration
├── init.sql              # Database initialization with user tables
└── pom.xml               # Maven dependencies with security libs
```

## System Architecture

The application follows a layered architecture with clear separation of concerns:

- **Controller Layer**: REST endpoints for authentication and scan management
- **Security Layer**: JWT-based authentication and authorization  
- **Service Layer**: Business logic for user management and URL scanning
- **Repository Layer**: Data access using Spring Data JPA
- **Entity Layer**: JPA entities with proper relationships and constraints

### Security Features

- **JWT Authentication**: Stateless token-based authentication
- **User Isolation**: All scan operations are automatically scoped to the authenticated user
- **Password Encryption**: BCrypt hashing for secure password storage
- **Input Validation**: URL format validation and required field checks
- **Error Handling**: Global exception handler with meaningful error messages
- **Database Security**: Foreign key constraints and user-scoped queries

### Database Schema

- **users**: User accounts with encrypted passwords and profile information
- **url_scans**: URL scan records linked to users with foreign key constraints
- **Indexes**: Optimized for common queries (user_id, status, created_at, external_scan_id)
- **User Data Isolation**: Database-level constraints ensure users can only access their own data

### Authentication Flow

1. User registers via `/api/auth/register` or logs in via `/api/auth/login`
2. Server returns JWT token containing user information
3. Client includes token in `Authorization: Bearer <token>` header for subsequent requests
4. `JwtAuthenticationFilter` validates token and sets security context
5. `SecurityUtils` extracts current user ID from security context
6. All repository queries automatically filter by user ID for data isolation

## End-to-End Usage Example

Here's a complete workflow showing how to use the API:

```bash
# 1. Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "developer@example.com", "password": "securepass123", "firstName": "Jane", "lastName": "Developer"}'

# Response: {"token": "eyJ...", "userId": 1, "email": "developer@example.com", ...}

# 2. Store the token for subsequent requests
TOKEN="eyJ..."

# 3. Submit a URL for scanning  
curl -X POST http://localhost:8080/api/scans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"url": "https://suspicious-site.com"}'

# Response: {"id": 1, "url": "https://suspicious-site.com", "status": "SUBMITTED", "userId": 1, ...}

# 4. List all your scans
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/scans

# 5. Check specific scan details
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/scans/1

# 6. Delete a scan when no longer needed
curl -X DELETE -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/scans/1
```

## Troubleshooting

### Authentication Issues
- Ensure JWT token is included in Authorization header with "Bearer " prefix
- Check token expiration (default 24 hours) - re-login if expired
- Verify user credentials for login
- Ensure Content-Type header is set to "application/json" for login/register

### API Access Issues  
- 404 errors on scans may indicate the scan doesn't exist or belongs to another user
- Validation errors return 400 with detailed field-level error messages
- Ensure URLs start with http:// or https://

### Database Connection Issues
- Ensure PostgreSQL is running and accessible
- Check database credentials and connection string
- Verify the database, user, and tables exist
- Run the init.sql script if tables are missing

### Port Already in Use
- Change the `SERVER_PORT` environment variable
- Or stop the service using the port

### Docker Issues
- Ensure Docker daemon is running
- Check available disk space
- Try `docker-compose down` and `docker-compose up --build` again

### Security Considerations for Production
- Change the default `JWT_SECRET` to a secure, long random string
- Use environment variables for all sensitive configuration
- Enable HTTPS in production
- Consider shorter JWT expiration times for enhanced security

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test locally using Docker Compose
5. Submit a pull request
