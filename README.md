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

## Households
Every record belongs to a household from household-api (`household-api-sb`), shared with the other
home apps. Each request names one in `X-Household-Id`; `home-household-spring` (from mavenLocal)
checks the signed-in member belongs to it in household-api's `HOUSEHOLD` database and hands the
controllers a `CurrentMember`: `428` without the header, `403` for a household they're not in.

- `BaseCrudRepository` extends `Repository`, not `MongoRepository`, and every method takes the
  household, so an unscoped query doesn't compile.
- Names are unique per household: a unique `(householdId, name)` index on every collection, created
  at startup (`app.mongo.create-indexes`, on by default).
- Caches are keyed by household.
- `BudgetPurger` deletes a household's records 30 days after it's deleted in Home.
- `HouseholdScopingTests` checks all of that against MongoDB in Testcontainers (needs Docker).
- `scripts/backfill-household.js` gave the records from before households to "My household".

Locally, the `local` profile signs every request in as `LOCAL_MEMBER_EMAIL` (default
`dev@example.com`), who needs a household in a local household-api like anyone else.

## Running locally
```bash
./gradlew test
./gradlew bootRun
# or in Docker — build the jar first, then build the image
./gradlew bootJar && docker build -t home-budget-sb .
```

## Configuration
Environment variables (see the Dockerfile for defaults): `MONGO_APP_URL`,
`MONGO_DATABASE`, `CORS_ORIGINS`, `SERVER_PORT` (default 9003), `HOUSEHOLD_DATABASE` (default
`HOUSEHOLD`), and `ACCESS_TEAM_DOMAIN` / `ACCESS_AUD` for Cloudflare Access (not needed with the
`local` profile).
