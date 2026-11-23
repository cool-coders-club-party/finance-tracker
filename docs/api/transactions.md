# Transactions service

**Service:** `transactions-service`  
**Local URL:** `http://localhost:8082`  
**Build:** Gradle wrapper, Java 21

Transactions owns users' income and expense records, categories, original currencies,
active/deleted history and converted financial summaries. Data is persisted with
Spring Data JPA in PostgreSQL; Liquibase manages the schema. Redis caches exchange
rates supplied by Frankfurter. The React dashboard and transaction forms consume
this service's authenticated API.

## Local setup

Prerequisites: Java 21 and Docker with Compose. From the repository root:

```bash
docker compose up -d transaction-postgres transaction-redis
cd backend/services/transactions-service
# Export JWT_SECRET with the same value used by the Users service before starting.
sh ./gradlew bootRun
```

`JWT_SECRET` is required and must contain at least 32 UTF-8 bytes. Use a private
local development value shared with Users; never commit real secrets. Alternatively,
place `JWT_SECRET=...` in an ignored `.env` file in the service's working directory.
`optional:file:.env[.properties]` is resolved relative to the process working directory,
so starting from this directory does not automatically load the repository-root `.env`.

The service starts on port **8082**. Its public health endpoint is
`GET /actuator/health`. Start the Users service and its database separately to
register/sign in and obtain a token. Transactions verifies JWTs locally; it does not
call Users for each request.

| Configuration | Default / requirement |
| --- | --- |
| `JWT_SECRET` | Required; same HS256 signing secret as Users |
| `POSTGRES_HOST` | `localhost` |
| `POSTGRES_PORT` | `5434` (Compose publishes container port 5432 here) |
| Database | `transactions`, schema `transaction` |
| Local database credentials | `postgres` / `postgres` in the development configuration |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` |
| `frankfurter.base-url` | `https://api.frankfurter.dev` |
| `frankfurter.connect-timeout` / `read-timeout` | `3s` / `5s` |
| `frankfurter.cache-ttl` | `1h` |

For the deployed Compose stack, see [Continuous Deployment](../poca-continuous-deployment.md).
Its environment and container hostnames differ from local IDE/Gradle startup.

## Authentication and ownership

All `/api/v1/transactions` endpoints, including metadata, require:

```http
Authorization: Bearer <accessToken>
```

Obtain `accessToken` from `POST /api/auth/login` on the Users service. Transactions
uses the token's `sub` claim as the user's UUID. Do not supply a `userId` query
parameter or request field to select an owner. Transaction IDs themselves are numeric.

Missing, expired or invalid tokens return **401**. Accessing another user's record
returns **404**, as does accessing a missing record. Security-layer responses need
not have the same JSON body as the application validation errors below.

## Endpoints

Paths below are relative to `/api/v1/transactions`.

| Method | Path | Behaviour | Success |
| --- | --- | --- | --- |
| POST | `/` | Create a transaction for the token owner | 201, transaction |
| GET | `/` | Page of the owner's active transactions | 200, page |
| GET | `/deleted` | Page of the owner's soft-deleted transactions | 200, page |
| GET | `/{id}` | Read an active transaction | 200, transaction |
| PATCH | `/{id}` | Update supplied fields of an active transaction | 200, transaction |
| DELETE | `/{id}` | Soft-delete an active transaction | 200, empty body |
| DELETE | `/{id}?permanent=true` | Permanently remove an owned active or deleted record | 200, empty body |
| POST | `/{id}/restore` | Restore an owned soft-deleted record | 200, transaction |
| GET | `/metadata` | Supported types, categories and currencies | 200, metadata |
| GET | `/summary?currency=EUR` | Convert and aggregate active transactions | 200, summary |

Soft-deleted records are excluded from normal history, individual reads, editing
and summaries. Restore makes them active again. Restoring an already-active or
permanently deleted record returns 404. Repeating soft deletion returns 404.
Permanent deletion cannot be undone through the restore endpoint.

### Create and edit

Example create body:

```json
{
  "transactionType": "EXPENSE",
  "transactionCategory": "GROCERIES",
  "currency": "EUR",
  "amount": "42.50",
  "description": "Weekly shopping",
  "occurredAt": "2026-01-15T12:30:00"
}
```

Required fields are type, category, currency, amount and `occurredAt`. Amount must
be at least `0.01`, with at most 17 integer digits and two fractional digits.
Description is optional and limited to 500 characters. Category must allow the
selected transaction type. `occurredAt` must be past or present.

`occurredAt` is a local ISO date-time without an offset (`LocalDateTime`), not a
UTC timestamp. History filters and exchange-rate dates use its recorded calendar
date. `createdAt` is assigned by the backend and cannot be edited through PATCH.

A transaction response contains:

```json
{
  "id": 42,
  "userId": "10000000-0000-0000-0000-000000000042",
  "transactionType": "EXPENSE",
  "transactionCategory": "GROCERIES",
  "currency": "EUR",
  "amount": 42.50,
  "description": "Weekly shopping",
  "occurredAt": "2026-01-15T12:30:00",
  "createdAt": "2026-01-15T12:31:00"
}
```

PATCH accepts the same editable fields, but only supplied non-null values change:

```json
{ "occurredAt": "2026-01-14T12:30:00", "description": "Corrected date" }
```

Omitted or null fields are preserved. Use `"description": ""` to clear a description.
An empty PATCH returns 400. Changing currency changes the recorded denomination;
it does **not** exchange or alter the original amount.

### Active and deleted history

Both endpoints accept identical filters:

```http
GET /api/v1/transactions?type=EXPENSE&category=GROCERIES&from=2026-01-01&to=2026-01-31&page=0&size=20&sortBy=date&direction=desc
```

| Parameter | Meaning |
| --- | --- |
| `type`, `category` | Optional enum filters; omit to include all types/categories |
| `from`, `to` | Optional inclusive `YYYY-MM-DD` bounds on `occurredAt`; either can be omitted |
| `page` | Zero-based, default `0`, minimum `0` |
| `size` | Default `20`, range `1`–`100` |
| `sortBy` | `date` (default), `occurredAt` (alias), or `amount` |
| `direction` | `desc` (default) or `asc` |

Sorting uses descending ID as a secondary key for stable ordering of equal values.
`from` must not be after `to`. Pages beyond the last page return empty content.

Page response shape:

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }
```

Non-empty `content` contains transaction objects in the format above. Fetch all
pages when a consumer needs the complete history; one page is not a financial total.

### Metadata

The response supplies `types`, `categories` and `currencies`. This abbreviated
example omits most categories:

```json
{
  "types": ["INCOME", "EXPENSE"],
  "categories": [
    { "name": "SALARY", "allowedTypes": ["INCOME"] },
    { "name": "GROCERIES", "allowedTypes": ["EXPENSE"] },
    { "name": "GIFT", "allowedTypes": ["INCOME", "EXPENSE"] }
  ],
  "currencies": ["EUR", "USD", "GBP"]
}
```

Forms should use the returned enum values, filter categories by `allowedTypes`,
and format readable labels without changing submitted values. Currency options
are strings, not `{code, name}` objects.

### Converted summaries

```http
GET /api/v1/transactions/summary?currency=EUR&from=2026-01-01&to=2026-01-31
```

`currency` is required (`EUR`, `USD` or `GBP`). Dates are optional and inclusive;
omitting both requests all-time totals. Only the token owner's active records count.

Example response:

```json
{
  "currency": "EUR",
  "income": 1000.00,
  "expenses": 42.50,
  "netIncome": 957.50,
  "spendingByCategory": [{ "category": "GROCERIES", "amount": 42.50 }]
}
```

- Original amounts and currencies stay unchanged in PostgreSQL.
- Conversion uses each transaction's `occurredAt` date and Frankfurter's ECB provider.
  Provider responses are checked for matching currencies, a positive rate, and an
  effective date no later than the requested date.
- Redis caches rates by currency pair and requested date. Cache failures fall back
  to the provider; a failed cache write does not discard a valid fetched rate.
- Same-currency amounts require no external exchange-rate request.
- Calculations use `BigDecimal`. Category totals and income are rounded to two
  places using `HALF_EVEN`; expenses sum the rounded categories, and net income is
  rounded income minus expenses. Displayed category figures therefore reconcile.
- If a required rate is unavailable, the whole summary returns 503 rather than
  silently omitting records. Saving transactions does not require exchange rates.
- No matching records returns zero totals and an empty category list.

## Errors

Application validation and transaction errors use `ApiErrorResponseDto`:

```json
{
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Validation failed",
  "path": "/api/v1/transactions",
  "details": { "amount": "must be greater than or equal to 0.01" }
}
```

`details` contains field/parameter errors when available, otherwise `{}`.
Invalid enum values, incompatible categories, invalid date ranges and pagination
parameters return 400. Missing/ineligible records return 404. Required exchange-rate
failures return 503. Authentication failures are handled separately by Spring Security.

## Implementation map

Paths are relative to
`backend/services/transactions-service/src/main/java/ie/universityofgalway/finance/transactions/`.

| Area | Main files | Responsibility |
| --- | --- | --- |
| HTTP API | `rest/controller/TransactionController.java`, `rest/dto/` | Token ownership, request validation and responses |
| Business logic | `service/TransactionServiceImpl.java` | Create, read, update, delete, restore and metadata |
| Summaries | `service/TransactionSummaryService.java` | Conversion and category aggregation |
| Persistence | `entity/Transaction.java`, `repository/TransactionRepository.java`, `repository/TransactionSpecifications.java` | JPA records, owner/status filtering and date predicates |
| Mapping | `mapper/TransactionMapper.java` | MapStruct DTO mapping and null-preserving PATCH updates |
| Currency | `currency/CurrencyConversionService.java`, `currency/ExchangeRateProvider.java` | Conversion and provider abstraction |
| External rates | `currency/FrankfurterExchangeRateProvider.java`, `currency/caching/CachingExchangeRateProvider.java` | HTTP rates and Redis cache decorator |
| Security/errors | `config/SecurityConfig.java`, `exception/TransactionExceptionHandler.java` | JWT verification and application error responses |

The React history page is `frontend/src/pages/TransactionHistory.jsx`; shared
create/edit fields are in `frontend/src/components/transactions/TransactionForm.jsx`.
The dashboard calls `/summary`, and `SpendingByCurrency.jsx` renders the category chart.

## Database migrations

Liquibase changesets are under `src/main/resources/liquibase/changesets`.
Hibernate validates the schema rather than creating it. Transactions store a
non-null UUID owner and an original currency alongside each amount.

## Tests

From the repository root, with Java 21 and Docker running:

```bash
cd backend/services/transactions-service
sh ./gradlew test
# Include application packaging:
sh ./gradlew build
```

Tests follow Given/When/Then naming and sections, using JUnit, AssertJ and Mockito.
Repository and integration tests run PostgreSQL through Testcontainers and production
Liquibase migrations. They do not use the running development database. The test
profile supplies a test-only JWT secret. Tests cover ownership, validation, metadata,
currencies, summaries, active/deleted history, pagination and restoration.

Reports: `build/reports/tests/test/index.html` (HTML) and `build/test-results/test/`
(JUnit XML). The [CI workflow](../../.github/workflows/poca-ci.yml) runs the Gradle
build and uploads test reports. The root Maven build does not run Transactions tests.
