@README.md

## Part of the home stack

This is the API for one of five home apps served at `home.ksl-apps.dev`. The stack (nginx,
Cloudflare Tunnel, every API) lives in `~/WebstormProjects/home-server`; read its `README.md` for
the full picture.

- Reached at `/budget-api/`, behind Cloudflare Access. Container `backend-home-budget`, config from
  `home-server/home/env/home-budget.env`.
- UI repo: `~/WebstormProjects/home-budget` (same name as this repo). A UI change belongs there.
- Households: `home-household-spring` (from mavenLocal) checks `X-Household-Id` against
  household-api's `HOUSEHOLD` database. A change to that check belongs in
  `~/IdeaProjects/home-household-spring`; a change to households themselves in
  `~/IdeaProjects/household-api-sb`.
- Deploy from `home-server`: `make deploy-budget` builds this jar and the UI, recreates the
  container, waits for it to be healthy and reloads nginx. The build skips tests (`-x test`), so run
  `./gradlew test` yourself first.

## Working here

- Gradle wrapper: `./gradlew build`, `./gradlew test`. Java 25, Spring Boot 4.
- Commit and push straight to `main`. Messages are plain sentences describing the change
  (`Keep each household's budget separate`).
