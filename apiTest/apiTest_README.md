# API Test Module

This module contains api tests that run against a Docker containerized version of the application. 
The tests make HTTP calls to verify the API endpoints are working correctly.

## Overview

The `apiTest` is a shell script that will:
- Build spring boot jarfile
- Build docker image
- Spin up the docker compose stack with required docker images
- Run apiTest gradle test

## Prerequisites

Before running the tests, ensure you have the following installed and configured:

### Required Software

1. **Java 25** (or higher)
   - Verify installation: `java -version`
   - Should show version 25 or higher

2. **Docker Desktop** (or Docker Engine)
   - Verify installation: `docker --version`
   - Docker must be running (check with `docker ps`)
   - Docker Compose V2 must be available: `docker compose version`

### System Requirements

- At least 4GB of free RAM (Docker containers need memory)
- Ports available: `8082` (application), `5432` (PostgreSQL), `9999` (WireMock)
- Sufficient disk space for Docker images

## Running Tests

### From the Root Directory

```bash
# Navigate to the apiTest directory
cd apiTest

# Build and run api tests
./build-and-run-apitest.sh
```

### What Happens When You Run Tests

1. **Builds the root project's bootJar** - The application JAR is built first
2. **Builds Docker images** - Creates the application Docker image
3. **Starts Docker containers**:
   - PostgreSQL database (port 5432)
   - Application server (port 8082)
   - WireMock server (port 9999)
   - Service Bus
4. **Runs tests** - Executes all test classes
5. **Stops and removes containers** - Cleanup after tests complete

## Test Reports

After running tests, you can view the results in several formats:

### HTML Test Report (Recommended)

**Location:** `apiTest/build/reports/tests/test/index.html`

The HTML report includes:
- Test summary (total, passed, failed, skipped)
- Individual test results with execution times
- Stack traces for failed tests
- Package and class-level summaries

## Troubleshooting

**Common causes:**
- Application configuration errors
- Database connection issues
- Missing environment variables
- Port conflicts

### Issue: Port already in use

**Solution:** Stop any services using the required ports:
```bash
# Check what's using port 8082
lsof -i :8082

# Check what's using port 5432
lsof -i :5432

# Stop conflicting services or change ports in docker-compose.yml
```

### Issue: Cannot connect to database

**Solution:**
- Ensure the database container is healthy: `docker ps` should show "Healthy"
- Check database logs: `docker-compose -f docker-compose.yml logs db`
- Verify connection string in `docker-compose.yml` matches database configuration


## Test Configuration

### Environment Variables

Tests use the following default configuration:
- Application base URL: `http://localhost:8082` (can be overridden with `app.baseUrl` system property)
- Database: PostgreSQL on port 5432
- WireMock: Port 9999



## Entra token validation against a local emulator

`EntraLocalTokenValidationApiTest` proves the whole authentication chain without a real tenant: the
`entra-local` container registers a subscriber application, issues it a client credentials token, and
`app-entra` verifies that token by fetching the emulator's JWKS over TLS.

`app1` is unchanged — it still runs with validation off and the locally minted tokens the rest of the
suite presents, so it proves nothing about verification. `app-entra` is the same image on port 8083
with `AUTH_MODE=ENFORCE`.

The emulator is [Entra Local](https://github.com/cmaneu/entra-local), patched in
`entra-local/Dockerfile` for two reasons:

- **`oid`.** Upstream deliberately omits it from app-only tokens. Real Entra sets it to the service
  principal object id, which for an app-only token equals `sub` — and `EntraTokenValidator`
  rejects a token without it as `DELEGATED_TOKEN`.
- **TLS.** Upstream generates a certificate for `localhost` only. The patched image generates one
  naming `entra-local` as well, plus a truststore, because the Nimbus JWKS fetch uses the JVM
  truststore and would otherwise reject the certificate and the hostname.

Where it still differs from a real tenant:

- **Role assignment.** The emulator grants a requesting application every app role defined on the
  resource. Real Entra needs an admin-consented app role assignment per client and issues a token
  carrying no `roles` without one.
- **`azpacr`** is not issued. Nothing validates it today.
- **One tenant**, fixed by `TENANT_ID`. `AUTH_AUDIENCE` uses the emulator's seeded `local-api`
  application (`cccccccc-0000-0000-0000-000000000007`) in place of this API's own registration.

```bash
# Roles and claims the emulator issued, decoded
docker compose logs entra-local | tail

# The emulator's portal, for inspecting registered applications and users
open https://localhost:9443
```
