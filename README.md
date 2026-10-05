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

Each service has its own build file, Spring Boot entry point, configuration, and isolated local H2 database. Five services use Maven; Transactions uses Gradle. The root Maven POM aggregates only the Maven services.

## Getting started

Use Java 21. From the repository root:

```bash
cd backend
./mvnw verify
cd services/transactions-service
./gradlew bootRun
```

That starts Transactions on `http://localhost:8082`; health is at `http://localhost:8082/actuator/health`.

Read the [microservices architecture note](docs/architecture-microservices.md), [Clean Architecture diagram](docs/clean-architecture.md), and [system architecture diagrams](docs/architecture-diagrams.md) before adding a feature. The original setup history remains in [the team brief](docs/team-brief-1-initializing-and-backend.md).
