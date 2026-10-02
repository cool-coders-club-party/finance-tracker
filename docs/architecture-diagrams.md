# Finance Tracker architecture diagrams

These diagrams describe the target architecture for Frontend1 and all six backend microservices. Frontend1 is the React application in [`frontend/`](../frontend/). Each backend service owns its data and exposes an API; services do not share databases.

## C4 context diagram

![Finance Tracker C4 context diagram](images/c4-context.svg)

```mermaid
C4Context
    title Finance Tracker - System Context

    Person(user, "User", "Tracks income, expenses, investments, and financial activity")
    System(frontend, "Frontend1", "React web application")
    System(finance, "Finance Tracker", "Financial tracking platform")
    System_Ext(market_provider, "Market Data Provider", "External prices and market information")

    Rel(user, frontend, "Uses")
    Rel(frontend, finance, "Uses through REST APIs")
    Rel(finance, market_provider, "Retrieves market data")
```

## C4 container diagram

![Finance Tracker C4 container diagram](images/c4-containers.svg)

```mermaid
C4Container
    title Finance Tracker - Container View

    Person(user, "User", "Finance Tracker user")
    Container(frontend, "Frontend1", "React", "Web UI for authentication, transactions, portfolio, insights, and reports")

    System_Boundary(finance, "Finance Tracker backend") {
        Container(users, "Users Service", "Spring Boot / Maven", "User accounts, profiles, and authentication")
        ContainerDb(users_db, "Users Database", "H2/PostgreSQL", "Users service data")

        Container(transactions, "Transactions Service", "Spring Boot / Gradle", "Income, expenses, categories, and transaction history")
        ContainerDb(transactions_db, "Transactions Database", "H2/PostgreSQL", "Transactions service data and Liquibase migrations")

        Container(portfolio, "Portfolio Service", "Spring Boot / Maven", "Holdings and portfolio calculations")
        ContainerDb(portfolio_db, "Portfolio Database", "H2/PostgreSQL", "Portfolio service data")

        Container(market_data, "Market Data Service", "Spring Boot / Maven", "Quotes, symbols, and market-data cache")
        ContainerDb(market_data_db, "Market Data Database", "H2/PostgreSQL", "Cached market data")

        Container(insights, "Insights Service", "Spring Boot / Maven", "Derived financial insights and recommendations")
        ContainerDb(insights_db, "Insights Database", "H2/PostgreSQL", "Insights service data")

        Container(reporting, "Reporting Service", "Spring Boot / Maven", "Reports and exports")
        ContainerDb(reporting_db, "Reporting Database", "H2/PostgreSQL", "Reporting service data")
    }

    System_Ext(market_provider, "Market Data Provider", "External prices and market information")

    Rel(user, frontend, "Uses")
    Rel(frontend, users, "Authenticates and manages profile", "HTTPS/JSON")
    Rel(frontend, transactions, "Creates and reads transactions", "HTTPS/JSON")
    Rel(frontend, portfolio, "Reads portfolio", "HTTPS/JSON")
    Rel(frontend, insights, "Reads insights", "HTTPS/JSON")
    Rel(frontend, reporting, "Requests reports", "HTTPS/JSON")

    Rel(users, users_db, "Reads/writes")
    Rel(transactions, transactions_db, "Reads/writes")
    Rel(portfolio, portfolio_db, "Reads/writes")
    Rel(market_data, market_data_db, "Reads/writes cache")
    Rel(insights, insights_db, "Reads/writes")
    Rel(reporting, reporting_db, "Reads/writes")

    Rel(portfolio, market_data, "Requests current prices", "HTTPS/JSON")
    Rel(insights, transactions, "Reads transaction data", "HTTPS/JSON")
    Rel(insights, market_data, "Reads current prices", "HTTPS/JSON")
    Rel(reporting, transactions, "Reads transaction data", "HTTPS/JSON")
    Rel(market_data, market_provider, "Retrieves quotes", "HTTPS")
```

## Sequence diagram: record a transaction and refresh the dashboard

![Finance Tracker dashboard sequence diagram](images/dashboard-sequence.svg)

This is the target request flow. The frontend calls the services directly; an API gateway can be introduced later if the team needs one public backend entry point.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Frontend1 as Frontend1 (React)
    participant Users as Users Service
    participant Transactions as Transactions Service
    participant Portfolio as Portfolio Service
    participant MarketData as Market Data Service
    participant Insights as Insights Service
    participant Reporting as Reporting Service

    User->>Frontend1: Sign in and open dashboard
    Frontend1->>Users: Authenticate user
    Users-->>Frontend1: Access token and user profile

    User->>Frontend1: Submit income or expense
    Frontend1->>Transactions: POST /api/v1/transactions
    Transactions->>Transactions: Validate request
    Transactions->>Transactions: Save transaction
    Transactions-->>Frontend1: 201 Created

    par Refresh transaction history
        Frontend1->>Transactions: GET transaction history
        Transactions-->>Frontend1: Transactions
    and Refresh portfolio
        Frontend1->>Portfolio: GET portfolio summary
        Portfolio->>MarketData: GET current prices
        MarketData-->>Portfolio: Quotes
        Portfolio-->>Frontend1: Portfolio summary
    and Refresh insights
        Frontend1->>Insights: GET financial insights
        Insights->>Transactions: Read transaction data
        Transactions-->>Insights: Transaction data
        Insights->>MarketData: Read current prices
        MarketData-->>Insights: Quotes
        Insights-->>Frontend1: Insights
    and Refresh report data
        Frontend1->>Reporting: GET report data
        Reporting->>Transactions: Read transaction data
        Transactions-->>Reporting: Transaction data
        Reporting-->>Frontend1: Report data
    end

    Frontend1-->>User: Updated dashboard
```

### Boundary notes

- Each service reads and writes only its own database.
- Cross-service data is requested through APIs; no service imports another service's Java classes.
- The diagram shows the intended system-wide design. Authentication, portfolio, insights, and reporting flows are planned integration points while their services are still being implemented.
