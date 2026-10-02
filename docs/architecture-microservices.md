# Microservices architecture

See the [system-wide C4 and sequence diagrams](architecture-diagrams.md) for Frontend1 and all backend services.

The original backend was one Spring Boot application with feature packages. It is now six independent services.

| Service | Port | Owns | Build tool |
|---|---:|---|---|
| `users-service` | 8081 | users and profiles | Maven |
| `transactions-service` | 8082 | transactions and categories | Gradle |
| `portfolio-service` | 8083 | holdings and portfolio calculations | Maven |
| `market-data-service` | 8084 | market prices and cache | Maven |
| `insights-service` | 8085 | financial insights | Maven |
| `reporting-service` | 8086 | reports and exports | Maven |

Each service has its own Spring Boot application, port, configuration, and local H2 database.

## Layout

```text
backend/
├── pom.xml                         # Maven aggregator; excludes Transactions
└── services/
    └── <service-name>/
        ├── pom.xml or build.gradle
        └── src/main/
            ├── java/.../<service>/
            └── resources/application.yml
```

Use clean architecture inside each service:

```text
<service>/
├── api/              # controllers and request/response DTOs
├── application/      # use cases and orchestration
├── domain/           # models and interfaces; no Spring/JPA annotations
└── infrastructure/   # repositories, HTTP clients, cache implementations
```

Dependencies point inward: `api → application → domain`. `infrastructure` implements interfaces defined in `domain`.

## Rules

1. Do not import Java code from another service.
2. Each service owns its database. Do not read or write another service's tables.
3. Services communicate through HTTP APIs. Events can be added later if needed.
4. The frontend may call services directly locally. Add an API gateway only when it is needed.
5. If one service needs data from another, call that service's API. Do not create a shared business-code module.
6. `users-service` will own authentication. Agree on the token approach before adding security to other services.

## Run locally

Build Maven services:

```bash
cd backend
./mvnw verify
```

Run one Maven service:

```bash
./mvnw -pl services/portfolio-service spring-boot:run
```

Run Transactions:

```bash
cd backend/services/transactions-service
./gradlew bootRun
```

Each service exposes `GET /actuator/health`.
