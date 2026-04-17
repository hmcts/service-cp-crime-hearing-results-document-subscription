# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this service does

Spring Boot 4.0 microservice that manages criminal court hearing event subscriptions. It receives PCR (Prison Court Register) and NOW (Notice of Wanting) events from the Progression service, retrieves documents from the Material service, and delivers HMAC-signed callback notifications to subscribers.

## Build & test commands

```bash
# Build
./gradlew clean build
./gradlew clean build -x test       # skip tests

# Test
./gradlew test                       # unit/integration tests
./gradlew test --tests uk.gov.hmcts.cp.subscription.services.SubscriptionServiceTest
./gradlew test --tests "*.SubscriptionServiceTest.testCreateClientSubscription"

# Code quality
./gradlew pmdMain                    # PMD static analysis (fails build on violations)
./gradlew jacocoTestReport           # coverage report → build/reports/jacoco/

# Run locally
./gradlew bootRun                    # starts on http://localhost:4550

# API tests (Docker required)
cd apiTest && ./build-and-run-apitest.sh
```

## Local setup

Requires PostgreSQL 15 on `localhost:5432/appdb` (user: `postgres`, password: `postgres`):

```bash
docker run -d --name postgres -p 5432:5432 \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=appdb postgres:15
```

Copy `.envrc.example` → `.envrc` (gitignored) to override env vars. Key overrides:

| Variable | Default |
|---|---|
| `SERVER_PORT` | `4550` |
| `DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/appdb` |
| `MATERIAL_CLIENT_URL` | `http://localhost:8081` |
| `DOCUMENT_SERVICE_URL` | `http://localhost:8082` |
| `AZURE_SERVICE_BUS_ENABLED` | `false` |

## Architecture

### Request flow

```
PCR event (from Progression)
  → NotificationController.createNotification()
    → [Service Bus queue if enabled, else synchronous]
    → NotificationManager.processPcrNotification()
      → NotificationService.processInboundEvent()
      → MaterialService.waitForMaterialMetadata()   ← polls with awaitility retry
      → DocumentService.saveDocumentMapping()
      → CallbackDeliveryService.submitOutboundPcrEvents()
        → per subscription: CallbackService.sendToSubscriber()  ← HMAC-signed POST
```

### Layer responsibilities

- **Controllers** — thin, implement OpenAPI-generated interfaces, extract client ID from MDC
- **Managers** (`NotificationManager`) — orchestrate multi-service workflows
- **Services** — single-responsibility domain logic
- **Clients** (`MaterialClient`, `CallbackClient`) — external HTTP calls with retry
- **Repositories** — Spring Data JPA; all queries must filter by `clientId`

### Multi-tenancy

`ClientIdResolutionFilter` extracts the client ID from the request and stores it in MDC under `ClientIdResolutionFilter.MDC_CLIENT_ID`. Every repository query must include this:

```java
subscriptionRepository.findByIdAndClientId(id, UUID.fromString(MDC.get(ClientIdResolutionFilter.MDC_CLIENT_ID)));
```

### OpenAPI-first

Controllers implement generated interfaces from the `api-cp-crime-hearing-results-document-subscription` dependency. Never edit generated code in `build/generated/`. To regenerate: `./gradlew openApiGenerate`.

### Async toggle

`AZURE_SERVICE_BUS_ENABLED=true` routes events through Azure Service Bus topics (`PCR_INBOUND_TOPIC`, `PCR_OUTBOUND_TOPIC`). When false, processing is synchronous on the same thread.

## Gradle structure

```
build.gradle                   ← plugins, dependencies (kept here for Dependabot)
gradle/
  dependencies/                ← shared version catalogs (java-core, spring-core, spring-db, etc.)
  github/test.gradle           ← Mockito agent setup, JaCoCo, failFast
  docker-test.gradle           ← dockerTest task (runs *Integration* tests with Docker)
  pmd.gradle                   ← PMD config
```

## Database migrations

Flyway runs automatically on startup. Add migrations to `src/main/resources/db/migration/` following Flyway naming: `V<VERSION>__<description>.sql`. To reset locally: stop/remove the PostgreSQL container and restart.

## CI/CD pipeline

```
GitHub push to main
  → ci-draft.yml (build + publish-artefact → Azure Artifacts hmcts-lib)
    → trigger-acr-copy (ADO Pipeline 460)
      → Pulls JAR from Azure Artifacts
      → Builds Docker image
      → Pushes to ACR as: hmcts/service-cp-crime-hearing-results-document-subscription-service
    → deploy-dev (ADO Pipeline 434 via hmcts/cp-vp-aks-deploy env/dev branch)
```

If `publish-artefact` fails with 409 (artifact already exists), it is treated as success so downstream jobs are not skipped.

## Key patterns

- **MapStruct mappers** in `subscription/mappers/` — don't edit generated `*Impl` classes
- **`@ConfigurationProperties`** via `AppProperties` for typed config binding
- **`@Slf4j`** for logging — INFO for business events, DEBUG for tracing
- **`ResponseStatusException`** for HTTP errors in business logic; `GlobalExceptionHandler` for cross-cutting
- **PMD ruleset** at `.github/pmd-ruleset.xml` — cyclomatic complexity and naming are common failure points

---

## Coding conventions

### Principles

Every line of code has a cost — keep it simple. Prefer less code, smaller classes, and higher cadence over speculative complexity.

- Keep classes and methods small, simple, and tested
- Follow Spring Boot defaults unless there is a strong reason to deviate; document deviations with a test that prevents accidental removal
- Do not add code that *might* be needed in future — add it when it is needed
- Remove code that is no longer used
- Only comment where the reason is non-obvious; do not add explanatory comments for standard patterns

### Immutability

- Prefer immutable objects; avoid setters — use builders instead
- Use `final` on fields (PMD enforces this in main code)
- Avoid string concatenation/manipulation; prefer typed values
- Use ternary expressions for simple conditional assignments
- Use private methods returning a fully built object when construction logic is non-trivial

### Code formatting

- Java: 4-space indentation
- YAML and Groovy (e.g. Gradle files): 2-space indentation
- Imports: set wildcard threshold to 99 (IntelliJ → Editor → Code Style → Java → Imports)

### Ordering

Order lists logically and consistently — not arbitrarily:

- Fields in a class: most important first, then by dependency order
- Constructor/method parameters: most important first, or by dependency (a client before the service that uses it)
- Injected dependencies: same ordering principle
- Apply the same principle to SQL columns, JSON fields, and enum values

### Method size

Target fewer than 20 lines per method. Extract well-named private methods for distinct sub-steps. Be aware that private method extraction can make test coverage harder — test the observable behaviour of the public entry point, not the private steps.

### Validation

- Validate all incoming data as tightly as possible at the boundary (controller or service entry point)
- Prefer typed values (`UUID`, `Long`, `LocalDateTime`) over plain `String`
- Where `String` is unavoidable, constrain with a regex pattern (e.g. `caseUrn`)
- Validate before calling any business-logic service

### Logging URLs/URIs

Sanitise before logging to prevent log injection:

```java
import org.owasp.encoder.Encode;

log.info("Calling {}", Encode.forJava(url));
```

### Feature toggles

- Expose toggles as application properties / environment variables, not dynamic runtime switches
- A toggle may start `false` while dependent services are not yet ready, and is switched on as part of a coordinated release
- Once enabled, toggles are not expected to be switched back off
- Remove the toggle and the conditional code as soon as it is permanently enabled

---

## Testing conventions

### Assertions

Use AssertJ (`assertThat(...)`) for all assertions.

### Test naming

Use the underscore pattern:

```
<method>_should_<expected outcome>[_when_<condition>]
```

Examples:
- `createSubscription_should_return_created_subscription`
- `processEvent_should_throw_not_found_when_material_missing`

### Writing tests

- Declare shared test objects at class level (treat them like imports — skip over them to find the test logic)
- Only populate fields that are relevant to the behaviour under test
- Do not verify mock interactions when the return value of a stubbed call is already asserted
- Do not repeat the same test with slightly different data unless the variation tests a distinct branch

---

## Commit message format

```
feature: amp-123 short imperative description

* Added class XyzService
* Removed unused method abc
```

- Type prefix: `feature:`, `fix:`, `chore:`, `refactor:`
- Ticket reference in lowercase: `amp-123`
- Bullet points for notable additions or removals