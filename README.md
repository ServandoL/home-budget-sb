# Home Budget — Spring Boot API

A household budgeting backend: track bills, income, debts, credit cards,
subscriptions, and house repairs in one place, with a dashboard view across
all of them.

## Tech stack
- Java 25, Spring Boot 4.0, Gradle (wrapper included)
- Spring Data MongoDB + MongoDB-backed sessions
- Caffeine caching (`spring-boot-starter-cache`)
- Bean Validation, Actuator, Lombok
- Docker (Eclipse Temurin 25 minimal image)
- Tests: JUnit + Mockito

## Architecture
A consistent layered design across every resource:

- `controllers/` — one controller per resource, built on a shared `CrudController`
- `services/` — business logic on a shared `BaseCrudService` / `BaseCrudServiceImpl`
- `repository/` — Spring Data repositories on a shared `BaseCrudRepository`
- `models/database/` — MongoDB documents (bills, credit cards, debts, incomes,
  subscriptions, house repairs) with enums for billing cycle, pay frequency,
  priority, and repair status
- `models/dto/` — separate create/update request DTOs and response DTOs per resource
- `exceptions/` — `RecordNotFoundException`, `RecordAlreadyExistsException`,
  and a `GlobalErrorHandler`

A `DashboardService` aggregates the resources into a single dashboard DTO
(covered by `DashboardServiceTest`).

## Running locally
```bash
./gradlew test
./gradlew bootRun
# or in Docker — build the jar first, then build the image
./gradlew bootJar && docker build -t home-budget-sb .
```

## Configuration
Environment variables (see the Dockerfile for defaults): `MONGO_APP_URL`,
`MONGO_DATABASE`, `CORS_ORIGINS`, `SERVER_PORT` (default 9003).
