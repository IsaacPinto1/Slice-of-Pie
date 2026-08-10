## Running the Project Locally

### 1. Start the Database

Start the local database using Docker:

```bash
docker compose up -d
```

To stop the database and remove all persisted data:

```bash
docker compose down -v
```

---

### 2. Start the Backend

Run the Spring Boot backend:

```bash
./mvnw spring-boot:run
```

Or run it with the `dev` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

### 3. Start the Frontend

Run the React development server:

```bash
npm run dev
```

The frontend should now be available at the URL shown in your terminal (typically `http://localhost:5173` for Vite).

---

## Configuration Profiles

The backend uses Spring profiles to switch configuration between local development, testing, and production. Each profile lives in `backend/src/main/resources/`:

- **`application.yml`** — Base configuration shared by every profile. All values here come from environment variables (`SPRING_DATASOURCE_URL`, `JWT_SECRET`, `FINNHUB_API_KEY`, etc.), so this file has no active-profile-specific defaults of its own.
- **`application-dev.yml`** — Used for local development. Points at the Postgres container started by the root `docker-compose.yml` (`dev_user` / `password` / `sliceofpie` on port `5432`) and enables verbose Spring Security logging. Activated by passing `-Dspring-boot.run.profiles=dev` when running the backend.
- **`application-test.yml`** — Used by the automated test suite. Points at the Postgres container started by `docker-compose.test.yml` (`test_user` / `test_password` / `sliceofpie_test`, also on port `5432`), with each value overridable via environment variable (this is what lets CI point at its own Postgres service). This profile is activated in code — every integration test class is annotated with `@ActiveProfiles("test")` — so you don't need to pass a profile flag when running `mvn test` locally.
- **`application-prod.yml`** — Used in production. Every value is sourced from environment variables, with minimal (`WARN`-level) logging.

**Note:** `docker-compose.yml` (dev) and `docker-compose.test.yml` (test) both publish Postgres on host port `5432`. Only one of the two containers can be running at a time unless you remap one of the ports yourself.

---

## Running the Test Suite Locally

The test suite is a set of Spring Boot integration tests that run against a real Postgres database, so a database needs to be up before you run them.

### 1. Start the test database

From the repo root, bring up the dedicated test Postgres container (separate from the dev database, and using a tmpfs volume so it starts empty every time):

```bash
docker compose -f docker-compose.test.yml up -d
```

### 2. Run the tests

```bash
cd backend
./mvnw test
```

This runs against the `test` profile automatically (via `@ActiveProfiles("test")` on the test classes), which talks to the database started in step 1.

### 3. Tear down the test database

```bash
docker compose -f docker-compose.test.yml down
```

### Running against a non-default database

If you need to point the test suite at a database with different credentials (for example, to mirror what CI does), set the corresponding environment variables before running `./mvnw test`:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sliceofpie_test
export SPRING_DATASOURCE_USERNAME=test_user
export SPRING_DATASOURCE_PASSWORD=test_password
```

These map directly to the `${...}` placeholders in `application-test.yml` and are the same variables set in `.github/workflows/ci.yml` for CI runs.