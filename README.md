# Finance Tracker

Finance Tracker is a collection of independently buildable Spring Boot services. The backend Maven reactor is in `backend`; business services live in `backend/services`.

| Service | Port | Owns |
|---|---:|---|
| users-service | 8081 | user identity and profiles |
| transactions-service | 8082 | income, expenses and transaction history |
| portfolio-service | 8083 | holdings and portfolio calculations |
| market-data-service | 8084 | market-price retrieval and caching |
| insights-service | 8085 | derived financial insights |
| reporting-service | 8086 | report generation and exports |

Each service has its own build file, Spring Boot entry point and configuration. Database requirements vary by service; Transactions uses PostgreSQL and Redis, while Market Data does not require a persistent database. Five services use Maven; Transactions uses Gradle. The root Maven POM aggregates only the Maven services.

## Getting started

Use Java 21. Build and test the Maven services from the repository root:

```bash
cd backend
sh ./mvnw verify
```

Transactions is built separately with Gradle. Its local startup requires PostgreSQL,
Redis and a `JWT_SECRET` shared with the Users service; follow the
[Transactions setup instructions](docs/api/transactions.md#local-setup).

Read the [microservices architecture note](docs/architecture-microservices.md), [Clean Architecture diagram](docs/clean-architecture.md), and [system architecture diagrams](docs/architecture-diagrams.md) before adding a feature. The original setup history remains in [the team brief](docs/team-brief-1-initializing-and-backend.md).

## Users

The Users service runs on port **8081** and handles registration, login and
session tokens. Passwords are hashed with BCrypt and users are stored in
PostgreSQL through Spring Data JPA. Login returns a short-lived JWT access
token (15 minutes, HS256) and a rotating refresh token (7 days), which is
stored only as a SHA-256 hash. Logging out revokes the refresh token.

The other services verify access tokens themselves with the shared
`JWT_SECRET` and identify the caller from the token's `sub` claim, so they
never call the Users service. The service follows Clean Architecture, with
dependencies pointing inward from `api` to `application` to `domain`.

See the [Users documentation](docs/api/users-service.md) for API endpoints, token
design, local setup and the demonstration checklist.

## Transactions

The Transactions service runs on port **8082** and manages each authenticated user's
income and expenses. It supports creation and editing, filtered and paginated
history, soft deletion, restoration and permanent deletion. Ownership comes from
the user's JWT, and records are persisted in PostgreSQL using JPA and Liquibase.

Transactions retain their original amount and currency (EUR, USD or GBP). The
metadata endpoint supplies form options, while the summary endpoint converts
income, expenses and category totals into the selected display currency using
transaction-date exchange rates from Frankfurter, cached in Redis. These summaries
power the dashboard cards and spending chart.

See the [Transactions documentation](docs/api/transactions.md) for local setup,
authentication, API examples, currency behaviour, implementation structure,
database migrations and testing.

## Market Data

The market-data service provides stock quotes, price history and
market overview data through a Spring Boot API on port 8084.
It supports fixed development prices through the `stub` profile
and Twelve Data prices through the `local` profile. Quotes are
cached in memory to reduce external API requests, with stale
cached quotes available as a fallback when the provider fails.

Portfolio-service uses these quotes to calculate holding values.
Market-data does not require a persistent database.

See the [Market Data documentation](docs/api/market-data.md)
for API endpoints, local setup, provider configuration and testing.

## Reporting

The Reporting service runs on port **8086** and combines transaction history and
current portfolio holdings into a UTF-8 CSV. Send a bearer JWT to
`GET /api/reports/export?from=YYYY-MM-DD&to=YYYY-MM-DD`. The date range is
inclusive, and transactions are ordered oldest first. Holdings with a positive
quantity are included regardless of the transaction date range.

See the [Reporting CSV export guide](backend/services/reporting-service/docs/reporting.md)
for the file layout and column definitions. To generate the service's JavaDoc,
run `sh ./mvnw -pl services/reporting-service javadoc:javadoc` from `backend`.

## Continuous Integration and Deployment

GitHub Actions runs the `Poca CI` workflow to check and build
the application. Póca is deployed on a T460 Ubuntu server using
Docker Compose, with Caddy serving the frontend and forwarding
API requests to backend containers. Cloudflare Tunnel provides
public access.

The server automatically checks `deploy/poca-demo`, approximately
one minute after each previous check or deployment finishes.
When a new commit has a successful push-triggered `Poca CI` run,
the server builds the application, updates its containers, checks
backend endpoints and publishes the frontend. Merging into `main`
alone does not update the live site.

The deployment combines `compose.yml` for databases and Redis
with `compose.demo.yml` for backend applications and deployment
settings. Server configuration is loaded from
`~/.config/poca/demo.env`, including the required `POCA_JWT_SECRET`.
Secret values must not be committed.

See the [Continuous Deployment documentation](docs/poca-continuous-deployment.md)
for the deployment sequence, server configuration, routing,
verification commands and known limitations.
