# Cache Invalidation Consistency Checker

## Overview

A production-grade **Cache Invalidation Consistency Checker** that simulates and analyzes cache-database consistency drift in distributed systems. This project demonstrates advanced DevOps practices including CI/CD pipeline automation, containerization, security scanning, and Kubernetes deployment.

## Problem Statement

In real-world distributed systems, cache and database often fall out of sync due to:
- Partial failures
- Race conditions
- Missed invalidations
- Network latency
- Concurrent updates

This system simulates these real-world cache consistency problems, detects drift, and provides auto-healing capabilities.

## Features

### Core Functionality
- **Version Tracking**: Track data versions in both DB and Cache
- **Drift Detection**: Identify stale cache entries automatically
- **Drift Score Calculation**: Calculate percentage-based drift metrics
- **Auto Refresh**: Automatically heal stale cache entries
- **Invalidation Logging**: Track all cache invalidation attempts
- **Failure Simulation**: Simulate real-world invalidation failures
- **REST APIs**: Complete API set for simulation and analysis

### DevOps Features
- **Complete CI/CD Pipeline**: GitHub Actions-based automation
- **Security Scanning**: SAST (CodeQL, SpotBugs) and SCA (OWASP Dependency Check)
- **Container Security**: Trivy vulnerability scanning
- **Code Quality**: Checkstyle linting
- **Comprehensive Testing**: Unit and integration tests
- **Kubernetes Deployment**: Production-ready K8s manifests
- **Health Monitoring**: Actuator endpoints for observability

## Architecture

```
┌─────────────────┐
│   REST API      │
│   Controllers   │
└────────┬────────┘
         │
┌────────▼─────────────────────────┐
│     Service Layer                │
│  ┌──────────┐  ┌──────────────┐ │
│  │ DBEngine │  │ CacheEngine  │ │
│  └──────────┘  └──────────────┘ │
│  ┌──────────────────────────┐   │
│  │  ConsistencyAnalyzer     │   │
│  └──────────────────────────┘   │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│  Data Layer                      │
│  ┌──────────┐  ┌──────────────┐ │
│  │ H2 DB    │  │ In-Memory    │ │
│  │          │  │ Cache        │ │
│  └──────────┘  └──────────────┘ │
└──────────────────────────────────┘
```

## Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Database**: H2 (in-memory)
- **Build Tool**: Maven
- **Containerization**: Docker
- **Orchestration**: Kubernetes
- **CI/CD**: GitHub Actions
- **Security**: CodeQL, SpotBugs, OWASP Dependency Check, Trivy

## API Endpoints

### Database APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/db/create` | Create new record |
| PUT | `/api/db/update/{id}` | Update existing record |
| GET | `/api/db/{id}` | Get record by ID |
| GET | `/api/db/all` | Get all records |
| DELETE | `/api/db/{id}` | Delete record |

### Cache APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/cache/{id}` | Get cached record |
| GET | `/api/cache/all` | Get all cached records |
| DELETE | `/api/cache/{id}` | Invalidate cache entry |
| DELETE | `/api/cache/clear` | Clear all cache |
| GET | `/api/cache/stats` | Get cache statistics |
| POST | `/api/cache/config/failure-rate` | Update failure rate |

### Analysis APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/analyze/drift?autoFix=true` | Generate drift report |
| GET | `/api/analyze/drift/summary` | Quick drift summary |
| GET | `/api/analyze/stale/{id}` | Check if record is stale |
| POST | `/api/analyze/refresh/{id}` | Force cache refresh |
| GET | `/api/analyze/events` | Get invalidation events |
| GET | `/api/analyze/events/recent` | Get recent events |
| GET | `/api/analyze/events/stats` | Get event statistics |

### Health & Monitoring
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Application health |
| GET | `/actuator/info` | Application info |
| GET | `/actuator/metrics` | Application metrics |

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker (optional)

### Build and Run

```bash
# Clone the repository
git clone <repository-url>
cd cache-consistency-checker

# Build the project
mvn clean package

# Run the application
java -jar target/cache-consistency-checker-1.0.0.jar

# Or run with Maven
mvn spring-boot:run
```

### Access the Application
- **Application**: http://localhost:8080
- **H2 Console**: http://localhost:8080/h2-console
- **Health Check**: http://localhost:8080/actuator/health

## Running with Docker

```bash
# Build Docker image
docker build -t cache-consistency-checker:latest .

# Run container
docker run -p 8080:8080 cache-consistency-checker:latest

# Check health
curl http://localhost:8080/actuator/health
```

## Kubernetes Deployment

### Prerequisites
- kubectl configured
- Kubernetes cluster (Minikube, Kind, GKE, EKS, or AKS)

### Deploy to Kubernetes

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Apply all manifests
kubectl apply -f k8s/

# Check deployment status
kubectl get pods -n cache-consistency
kubectl get services -n cache-consistency

# Access the application (NodePort)
kubectl port-forward service/cache-consistency-checker 8080:80 -n cache-consistency
```

## CI/CD Pipeline

### GitHub Secrets Configuration

Configure the following secrets in your GitHub repository:

```
DOCKERHUB_USERNAME: Your DockerHub username
DOCKERHUB_TOKEN: Your DockerHub access token
```

### CI Pipeline Stages

1. **Checkout & Setup**: Code checkout and dependency caching
2. **Linting**: Checkstyle code quality checks
3. **SAST - CodeQL**: Static Application Security Testing
4. **SAST - SpotBugs**: Bug detection analysis
5. **SCA**: OWASP Dependency vulnerability check
6. **Unit Tests**: Comprehensive test execution
7. **Build**: Maven package creation
8. **Docker Build**: Container image creation
9. **Image Scan**: Trivy security scanning
10. **Container Test**: Runtime validation
11. **Docker Push**: Registry publication

### CD Pipeline Stages

1. **Deploy**: Kubernetes deployment
2. **DAST**: Dynamic Application Security Testing
3. **Smoke Tests**: Post-deployment validation

## Testing Strategy

### Unit Tests
```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=CacheEngineTest

# Run with coverage
mvn clean test jacoco:report
```

### Integration Tests
```bash
# Run integration tests
mvn verify

# Check reports
open target/site/jacoco/index.html
```

## Configuration

### Application Properties

Key configuration properties in `application.properties`:

```properties
# Cache Simulation
cache.simulation.failure-rate=0.2
cache.simulation.network-delay-ms=100
cache.ttl.default-seconds=300

# Database
spring.datasource.url=jdbc:h2:mem:cachedb

# Logging
logging.level.com.devops.cache=DEBUG
```

## Example Usage

### Simulate Cache Drift

```bash
# 1. Create a record
curl -X POST http://localhost:8080/api/db/create \
  -H "Content-Type: application/json" \
  -d '{"value": "test-data", "cacheImmediately": true}'

# 2. Update the record with failed invalidation
curl -X PUT http://localhost:8080/api/db/update/1 \
  -H "Content-Type: application/json" \
  -d '{"value": "updated-data", "invalidateCache": true, "simulateFailure": true}'

# 3. Check drift
curl http://localhost:8080/api/analyze/drift

# 4. Auto-fix drift
curl http://localhost:8080/api/analyze/drift?autoFix=true
```

## Drift Score Interpretation

| Score | Verdict | Description |
|-------|---------|-------------|
| 0-10% | HEALTHY | System is healthy |
| 11-30% | MINOR_DRIFT | Minor inconsistencies detected |
| 31-60% | RISK | Significant risk of stale data |
| 61-100% | CRITICAL | Critical consistency failure |

## Project Structure

```
cache-consistency-checker/
├── .github/
│   └── workflows/
│       ├── ci.yml                 # CI Pipeline
│       └── cd.yml                 # CD Pipeline
├── k8s/
│   ├── namespace.yaml             # K8s Namespace
│   ├── configmap.yaml             # Configuration
│   ├── deployment.yaml            # Deployment spec
│   └── service.yaml               # Service spec
├── src/
│   ├── main/
│   │   ├── java/com/devops/cache/
│   │   │   ├── model/             # Domain models
│   │   │   ├── repository/        # Data repositories
│   │   │   ├── service/           # Business logic
│   │   │   ├── controller/        # REST controllers
│   │   │   ├── dto/               # Data transfer objects
│   │   │   └── CacheConsistencyCheckerApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/                      # Unit tests
├── Dockerfile                     # Container definition
├── pom.xml                        # Maven configuration
└── README.md                      # This file
```

## Security Considerations

### Container Security
- Non-root user execution
- Read-only filesystem where possible
- Minimal base image (Alpine)
- Vulnerability scanning with Trivy

### Application Security
- Input validation
- Security headers
- Actuator endpoints protection
- Dependency vulnerability scanning

## Performance Optimization

- Maven dependency caching
- Docker layer caching
- Connection pooling
- Efficient query patterns
- Resource limits in K8s

## Troubleshooting

### Common Issues

**Issue**: Application fails to start
```bash
# Check logs
docker logs <container-id>
kubectl logs <pod-name> -n cache-consistency
```

**Issue**: Tests failing
```bash
# Run with verbose output
mvn test -X
```

**Issue**: Docker build fails
```bash
# Clear Docker cache
docker builder prune -a
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit changes
4. Push to the branch
5. Create a Pull Request

## License

This project is created for educational purposes as part of the DevOps CI/CD assessment.

## Author

Ayush Kesharwani
Scaler DevOps Program

## Acknowledgments

- Spring Boot Team
- GitHub Actions
- OWASP Foundation
- Kubernetes Community
