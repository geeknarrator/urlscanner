@echo off
REM URL Scanner - Windows Setup Script for Code Reviewers

echo ================================== 
echo  URL Scanner - Automated Setup
echo ==================================
echo.

REM Check Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not installed. Please install Docker Desktop first.
    echo Visit: https://docs.docker.com/desktop/windows/
    pause
    exit /b 1
)

REM Check Docker Compose
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Compose is not installed. Please install Docker Compose first.
    pause
    exit /b 1
)

echo [INFO] Prerequisites check passed!

REM Clean up existing containers
echo [INFO] Cleaning up existing containers...
docker-compose down -v --remove-orphans >nul 2>&1

REM Generate .env file with sample secrets
echo [INFO] Generating configuration...
(
echo # Auto-generated for Windows code review
echo # Generated on: %date% %time%
echo.
echo DB_HOST=localhost
echo DB_PORT=5432
echo DB_NAME=urlscanner
echo DB_USER=urlscanner
echo DB_PASSWORD=secure-review-password-123
echo.
echo SERVER_PORT=8080
echo.
echo JWT_SECRET=windows-review-jwt-secret-key-that-is-long-enough-for-hs256-algorithm
echo JWT_EXPIRATION=86400000
echo.
echo URLSCAN_API_KEY=demo-api-key-for-review
echo SPRING_PROFILES_ACTIVE=demo
) > .env

echo [SUCCESS] Configuration created!

REM Build and start services
echo [INFO] Building and starting services (this may take a few minutes)...
docker-compose up --build -d

if errorlevel 1 (
    echo [ERROR] Failed to start services
    pause
    exit /b 1
)

REM Wait for services
echo [INFO] Waiting for services to start...
timeout /t 15 /nobreak >nul

REM Create sample user
echo [INFO] Creating sample user...
curl -s -X POST http://localhost:8080/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"reviewer@example.com\",\"password\":\"reviewer123\",\"firstName\":\"Code\",\"lastName\":\"Reviewer\"}" >nul 2>&1

echo.
echo ==================================
echo   Setup Complete!
echo ==================================
echo.
echo Application URL: http://localhost:8080
echo.
echo Sample User:
echo   Email: reviewer@example.com
echo   Password: reviewer123
echo.
echo Test Commands:
echo   Health Check: curl http://localhost:8080/actuator/health
echo.
echo Management:
echo   View logs: docker-compose logs -f
echo   Stop: docker-compose down
echo.
echo Happy Code Reviewing!
echo.
pause