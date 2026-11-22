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
(covered by `DashboardServiceTest`). Each paycheck moves the same share of itself to the bills
account, so the paydays every month brings (two biweekly, four weekly) pay the larger of the target
and the bills. An extra payday (a third biweekly or fifth weekly one) moves only the pay period
buffer, the cushion that grows the account a little each payday. When two paychecks can't cover the
bills, every payday moves the same amount and `shortMonthsCushion` is what the short months can draw.

## Households
Every record belongs to a household from household-api (`household-api-sb`), shared with the other
home apps. Each request names one in `X-Household-Id`; `home-household-spring` (from mavenLocal)
checks the signed-in member belongs to it in household-api's `HOUSEHOLD` database and hands the
controllers a `CurrentMember`: `428` without the header, `403` for a household they're not in.

- `BaseCrudRepository` extends `Repository`, not `MongoRepository`, and every method takes the
  household, so an unscoped query doesn't compile.
- Names are unique per household: a unique `(householdId, nameIndex)` index on every collection,
  created at startup (`app.mongo.create-indexes`, on by default). See Encryption.
- Caches are keyed by household.
- The planning settings (target monthly transfer, pay period buffer) are one `BudgetSettings`
  record per household, at `/api/v1/settings` (GET, PUT, DELETE to reset), with both amounts
  encrypted. Until a household saves its own: a calculated target and a $100 buffer. The dashboard
  always uses them; it takes no parameters.
  `EncryptingMongoListener` covers every entity, so the settings' `@Encrypted` amounts are too.
- `BudgetPurger` deletes a household's records, and its settings, 30 days after it's deleted in Home.
- `HouseholdScopingTests` and `BudgetSettingsTests` check all of that against MongoDB in Testcontainers (needs Docker).
- `scripts/backfill-household.js` gave the records from before households to "My household".

## Encryption
Names, amounts, balances, APRs, costs, descriptions and notes are stored encrypted (AES-256-GCM), so
someone reading the database sees `Binary.createFromBase64(…, 128)` instead of `Rent: 1500`. The API
encrypts on save and decrypts on read; everything above the repositories sees plain values.

- A field marked `@Encrypted` is encrypted by `EncryptingMongoListener`. Ids, households, dates, due
  days, enums, priorities and status stay plain, so they can still be queried.
- MongoDB can't filter or sort an encrypted field: sort in Java (`BaseCrudServiceImpl.getAll` with a
  `Comparator`), as bills, credit cards, debts and incomes do.
- Names are found and kept unique through `nameIndex`, a keyed hash of household and name, with a
  unique `(householdId, nameIndex)` index.
- At startup, records saved before encryption are encrypted (`EncryptionMigration`), before the
  index is built.
- The key is `BUDGET_ENCRYPTION_KEY`: 32 random bytes in base64 (`openssl rand -base64 32`). The API
  won't start without it. **Without the key the data can't be recovered**, from the database or the
  backups, so keep a copy somewhere other than the server.

### Debugging encrypted records
`BudgetInspect` prints records decrypted. It only reads. Give it a collection, and optionally a
household and an exact name:

```bash
# Inside the running container, which already has the key and connection string:
docker exec backend-home-budget java -Dloader.main=com.servando.homebudget.tools.BudgetInspect \
  -cp /app/app.jar org.springframework.boot.loader.launch.PropertiesLauncher Bills <householdId> [name]

# From a checkout, with MONGO_APP_URL, MONGO_DATABASE and BUDGET_ENCRYPTION_KEY set:
./gradlew -q inspect --args="Bills <householdId> [name]"
```

In Atlas or mongosh, filter on the plain fields (`householdId`, `_id`, dates, `billingCycle`, …), then
read that household's records with `BudgetInspect`.

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
`HOUSEHOLD`), `BUDGET_ENCRYPTION_KEY` (required; the `local` profile has a dev key), and `ACCESS_TEAM_DOMAIN` / `ACCESS_AUD` for Cloudflare Access (not needed with the
`local` profile).
