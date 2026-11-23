# Portfolio API Contract

**Owner:** Niall Lydon  
**Service:** `portfolio-service`  
**Local URL:** `http://localhost:8083`  
**Through the frontend:** `/portfolio-api/...` (the Vite dev proxy and Caddy both remove the `/portfolio-api` prefix)

The portfolio service records what a user owns: their portfolios, the holdings in each one, and every buy and sell. It values holdings using quotes from market-data-service, and calculates profit and loss from trade history.

## Contents

- [Authentication and ownership](#authentication-and-ownership)
- [Endpoints](#endpoints)
- [Errors](#errors)
- [How it works](#how-it-works)
- [Differences from the #100 handoff](#differences-from-the-100-handoff)
- [Run locally](#run-locally)
- [Tests](#tests)
- [Demonstration checklist](#demonstration-checklist)

## Authentication and ownership

Every endpoint requires an access token from users-service:

```http
Authorization: Bearer <accessToken>
```

The service checks the token's signature with the shared `JWT_SECRET` and uses the token's subject as the user id. A user id is never accepted from the request body or URL.

A portfolio that belongs to someone else returns `404 Not Found`, the same as a portfolio that doesn't exist, so the API never reveals which portfolio ids are in use.

## Endpoints

All paths start with `/api/portfolios`.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/portfolios` | List the caller's portfolios |
| `POST` | `/api/portfolios` | Create a portfolio |
| `GET` | `/api/portfolios/{id}/holdings` | Holdings, without prices |
| `GET` | `/api/portfolios/{id}/valuation` | Holdings valued at current market prices, with totals |
| `GET` | `/api/portfolios/{id}/trades` | Every buy and sell, oldest first |
| `GET` | `/api/portfolios/{id}/performance` | Profit and loss per stock, replayed from trades |
| `POST` | `/api/portfolios/{id}/buy` | Buy at the market price, or record an earlier purchase |
| `POST` | `/api/portfolios/{id}/sell` | Sell at the market price |

Decimal values (quantities, prices, totals) are JSON numbers backed by `BigDecimal`. Times are ISO-8601 UTC timestamps.

### List portfolios

```http
GET /api/portfolios
```

```json
[
  { "id": 1, "name": "My first portfolio" }
]
```

Returns `[]` when the user has none. The Portfolio page and the dashboard both use the first portfolio. The reporting service also uses this endpoint to find a user's portfolios.

### Create a portfolio

```http
POST /api/portfolios
Content-Type: application/json
```

```json
{ "name": "My first portfolio" }
```

Returns `201 Created` with a `Location: /api/portfolios/{id}` header and the new portfolio:

```json
{ "id": 1, "name": "My first portfolio" }
```

`name` is required and must be at most 255 characters.

### Holdings

```http
GET /api/portfolios/{id}/holdings
```

```json
[
  { "id": 3, "ticker": "AAPL", "quantity": 13, "avgPurchasePrice": 192.45, "currency": "USD" }
]
```

`avgPurchasePrice` is the weighted average price paid per share. `currency` can be `null` for holdings created before the currency column existed.

Holdings that have been sold down to zero shares stay in the database with `quantity: 0`, so their trade history keeps its link. The frontend hides them.

### Valuation

```http
GET /api/portfolios/{id}/valuation
```

```json
{
  "holdings": [
    {
      "id": 3,
      "ticker": "AAPL",
      "quantity": 13,
      "avgPurchasePrice": 192.45,
      "currency": "USD",
      "quote": { "symbol": "AAPL", "price": 201.10, "currency": "USD", "asOf": "2026-11-19T14:30:00Z", "stale": false },
      "valuationAvailable": true,
      "unavailableReason": null,
      "costBasis": 2501.85,
      "currentValue": 2614.30,
      "unrealisedGainLoss": 112.45
    }
  ],
  "currency": "USD",
  "complete": true,
  "stale": false,
  "unavailableHoldings": 0,
  "costBasis": 2501.85,
  "currentValue": 2614.30,
  "unrealisedGainLoss": 112.45
}
```

- A holding that can't be valued has `valuationAvailable: false`, a reason, and `null` money fields.
- The reason is one of `QUOTE_UNAVAILABLE`, `INVALID_QUOTE`, `CURRENCY_MISSING` or `CURRENCY_UNSUPPORTED`.
- Only USD holdings are valued until a currency-conversion policy exists.
- If any holding can't be valued, `complete` is `false` and the portfolio totals are `null`. A partial sum is never presented as the whole portfolio's value.
- `stale: true` means at least one quote came from market-data's cache after the live provider failed. Show each quote's `asOf` time.

This endpoint was designed and built by Jakub Zeman in #101; see [Portfolio valuation](portfolio-valuation.md).

### Trades

```http
GET /api/portfolios/{id}/trades
```

```json
[
  { "id": 10, "ticker": "AAPL", "type": "BUY",  "quantity": 15, "price": 192.45, "executedAt": "2026-11-17T14:32:00Z" },
  { "id": 11, "ticker": "AAPL", "type": "SELL", "quantity": 2,  "price": 195.00, "executedAt": "2026-11-18T10:58:13Z" }
]
```

Ordered by `executedAt`, oldest first. A purchase recorded with an earlier date appears at that date, not at the time it was entered.

### Performance

```http
GET /api/portfolios/{id}/performance
```

```json
[
  {
    "ticker": "AAPL",
    "quantity": 13,
    "avgPurchasePrice": 192.4500,
    "currentPrice": 201.10,
    "costBasis": 2501.85,
    "marketValue": 2614.30,
    "unrealizedPnl": 112.45,
    "unrealizedPnlPercent": 4.49,
    "realizedPnl": 5.10
  }
]
```

One entry per stock ever traded, sorted by ticker. The figures are rebuilt by replaying that stock's trades in date order:

- **`realizedPnl`** is the profit locked in by past sales.
- **Closed positions** (no shares left) have a `null` `currentPrice`, zero unrealised figures and their final realised profit or loss.

The dashboard's **Realised Profit** card is the sum of `realizedPnl` across every entry.

### Buy

```http
POST /api/portfolios/{id}/buy
Content-Type: application/json
```

**Buy now at the market price:**

```json
{ "ticker": "AAPL", "quantity": 2 }
```

The service fetches the current quote from market-data-service and records the trade at that price, timestamped now.

**Record a purchase made earlier,** for example shares bought yesterday and entered today:

```json
{ "ticker": "AAPL", "quantity": 3, "price": 180.00, "executedAt": "2026-11-17T14:32:00Z" }
```

The trade is recorded at the price actually paid and at the given time.

| Field | Required | Rules |
| --- | --- | --- |
| `ticker` | Yes | 1–5 letters; any case, stored upper case. Must be a symbol market-data recognises. |
| `quantity` | Yes | Greater than zero; fractional shares allowed. |
| `price` | Only when recording an earlier purchase | Greater than zero. Send together with `executedAt`. |
| `executedAt` | Only when recording an earlier purchase | Not in the future (one minute of clock difference is allowed). Not earlier than your most recent sale of the same stock. |

Send both `price` and `executedAt`, or neither. Sending only one is rejected.

Returns `200 OK` with the updated holding, in the same shape as an item from [Holdings](#holdings).

### Sell

```http
POST /api/portfolios/{id}/sell
Content-Type: application/json
```

```json
{ "ticker": "AAPL", "quantity": 2 }
```

Sells at the current market price, now. Returns `200 OK` with the updated holding.

Selling more shares than you hold, or a stock you don't hold, returns `409 INSUFFICIENT_SHARES` and changes nothing. Backdated sales aren't supported: sending `price` or `executedAt` returns `400`.

## Errors

Errors from the endpoints have this shape:

```json
{ "error": "INSUFFICIENT_SHARES", "message": "Cannot sell 5 AAPL: only 2 held" }
```

| Status | `error` | When |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | A field is missing or invalid, a backdated purchase breaks a rule, or `price`/`executedAt` are sent with a sale. The message says which field or rule. |
| `400` | `INVALID_SYMBOL` | Market-data doesn't recognise the ticker |
| `404` | `NOT_FOUND` | The portfolio doesn't exist or belongs to someone else |
| `409` | `INSUFFICIENT_SHARES` | Selling more shares than are held |
| `503` | `PRICE_UNAVAILABLE` | Market-data is unreachable or returned an unusable quote, so a buy, sell or performance request couldn't be priced. Valuation doesn't fail: it marks affected holdings unavailable instead. |

A missing, invalid or expired token returns `401` with a slightly different shape (`code` instead of `error`):

```json
{ "code": "UNAUTHORIZED", "message": "Authentication is required to access this resource" }
```

## How it works

### Layers

The service follows the team's [Clean Architecture](../clean-architecture.md) layout:

| Package | Contains |
| --- | --- |
| `api` | Controllers, request/response DTOs and `GlobalExceptionHandler`. Translates HTTP to and from the application layer. |
| `application` | `PortfolioService` (portfolios, buy, sell), `ValuationService`, `PerformanceService`, and the ports (interfaces) they depend on. |
| `domain` | `Portfolio`, `Holding`, `Trade`, `MarketQuote`, `PerformanceCalculator` and the domain exceptions. No Spring or database code. |
| `infrastructure` | JPA entities and repository adapters, the market-data price client, and security configuration. |

### Data

PostgreSQL holds three tables, created and updated by Hibernate (`ddl-auto: update`):

- **`portfolios`:** id, owner's user id, name.
- **`holdings`:** one row per stock per portfolio (unique on `portfolio_id` + `ticker`), with quantity, weighted average price and currency.
- **`trades`:** every buy and sell, linked to its holding, with type, quantity, price and time.

A buy or sell updates the holding and saves the trade in one transaction. The holding is saved first because a trade must point at an existing holding.

### Prices

`PriceClient` is the port for prices. Two implementations exist, chosen by `price-client.type`:

- **`market-data`** (default): `MarketDataPriceClient` calls market-data-service's `GET /api/quotes/{symbol}`. It uses a 1-second connect timeout and a 2-second read timeout, so a slow provider fails fast with a `503`.
- **`fake`:** `FakePriceClient` uses fixed USD prices for `AAPL` and `MSFT` only, for running without market-data. It's never active under the `prod` profile.

### Currency

A new holding takes its currency from the market quote at the time of its first purchase. This is also true when recording an earlier purchase: the quote confirms the ticker exists and supplies the currency.

Without a currency, valuation reports the holding as `CURRENCY_MISSING`. Holdings created before #101 added the column have a `null` currency and stay unvalued until their real purchase currency is confirmed. Don't fill it in with USD just because today's quote is in USD.

### Why a purchase can't be dated before a later sale

The holding stores a running weighted average price, updated each time a trade is saved. The performance endpoint instead replays trades in date order.

If a purchase were dated before an existing sale, that sale's average cost would differ between the two calculations, and they would disagree. So a backdated purchase must be dated after the most recent sale of the same stock. Purchases dated before any sale, or between purchases, are fine.

### No CORS configuration

Browsers always reach this service through a proxy on the same site: Vite's `/portfolio-api` proxy in development, Caddy when deployed. So the service has no CORS rules, and `SecurityConfig` disables CORS.

An earlier localhost-only CORS allow-list made every `POST` from the deployed site fail with `403`, because browsers send an `Origin` header with writes even to their own site. This was fixed in #144. As a side effect, browsers on other websites can't call the API.

## Differences from the #100 handoff

Jakub's handoff in [Portfolio valuation](portfolio-valuation.md#handoff-for-niall-100) proposed a purchase contract. The implementation differs in these ways:

| Handoff proposal | As built | Reason |
| --- | --- | --- |
| Fields `pricePerShare`, `currency`, `purchasedAt` | Fields `price`, `executedAt`; currency comes from the market quote | Shorter names. Taking the currency from the quote avoids trusting the client to name it, and only USD can be valued anyway. |
| Every buy records a purchase with its price and time | Two modes: buy now at market price, or record an earlier purchase | Keeps the existing market-price buy (used by the curl examples and the Buy button), while adding recording for purchases made elsewhere. |
| Recording must work when market-data is offline | Recording still asks market-data for the ticker and currency | Without the quote, an unknown ticker couldn't be rejected and the holding would have no currency. Possible follow-up: accept `currency` in the request and skip the quote when it's given. |
| Legacy null-currency holdings: reject adding to them until corrected | Adding to a legacy holding is allowed; its currency stays `null` and it stays unvalued | The holding still records shares correctly. Correcting its currency remains a data fix. |

Sell recording stays at market price only, as the handoff scoped it.

## Run locally

Requirements: Java 21, Docker, and a `JWT_SECRET` of at least 32 bytes that matches users-service.

1. **Start the database** from the repository root. It runs PostgreSQL on port `5436`, with database, user `portfolio` and password `portfolio_dev`:
   ```bash
   docker compose up -d portfolio-db
   ```
2. **Start users-service** (port `8081`) so you can log in, and **market-data-service** (port `8084`) for prices. See [Market Data](market-data.md#run-locally) for its `stub` and `local` profiles. In stub mode only `AAPL`, `MSFT` and `TSLA` have prices.
3. **Start portfolio-service** from the `backend` folder:
   ```bash
   JWT_SECRET=<same secret as users-service> ./mvnw -pl services/portfolio-service spring-boot:run
   ```
   In IntelliJ, add `JWT_SECRET` to the run configuration's environment variables instead.
4. **Start the frontend** (`cd frontend && npm run dev`) and open the Portfolio page.

To run without market-data, start with `--price-client.type=fake`. Only `AAPL` and `MSFT` can then be bought or valued; other holdings show as unavailable.

### Configuration

| Setting | Default | Purpose |
| --- | --- | --- |
| `PORTFOLIO_DB_URL` | `jdbc:postgresql://localhost:5436/portfolio` | Database connection |
| `PORTFOLIO_DB_USER` / `PORTFOLIO_DB_PASSWORD` | `portfolio` / `portfolio_dev` | Database login |
| `JWT_SECRET` | none (required) | Verifies access tokens; must match users-service |
| `price-client.type` | `market-data` | `market-data` or `fake` |
| `market-data.base-url` | `http://localhost:8084` | Market-data address (`http://market-data-service:8084` in deployment) |
| `server.port` | `8083` | HTTP port |

### Trying the API with curl

Log in to get a token, then call the service directly. The first command uses `jq` to pull out `accessToken`; without `jq`, copy it from the login response by hand.

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"<your password>"}' | jq -r .accessToken)

# Create a portfolio and note its id
curl -i -X POST http://localhost:8083/api/portfolios \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"My first portfolio"}'

PID=1

# Buy at market price
curl -i -X POST http://localhost:8083/api/portfolios/$PID/buy \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"ticker":"AAPL","quantity":10}'

# Record an earlier purchase
curl -i -X POST http://localhost:8083/api/portfolios/$PID/buy \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"ticker":"AAPL","quantity":3,"price":180.00,"executedAt":"2026-11-17T14:32:00Z"}'

# Sell
curl -i -X POST http://localhost:8083/api/portfolios/$PID/sell \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"ticker":"AAPL","quantity":5}'

# Read
curl -i http://localhost:8083/api/portfolios/$PID/valuation   -H "Authorization: Bearer $TOKEN"
curl -i http://localhost:8083/api/portfolios/$PID/trades      -H "Authorization: Bearer $TOKEN"
curl -i http://localhost:8083/api/portfolios/$PID/performance -H "Authorization: Bearer $TOKEN"
```

On Windows Command Prompt, use `set TOKEN=...` and `%TOKEN%`, and escape the JSON quotes as `\"`.

## Tests

From the `backend` folder:

```bash
./mvnw -pl services/portfolio-service test
```

| Test class | Covers |
| --- | --- |
| `PortfolioServiceTest` | Buy, sell, backdated purchases and their rules, ownership checks |
| `HoldingTest`, `PortfolioTest` | Weighted average price and sale rules on the domain models |
| `PerformanceCalculatorTest`, `PerformanceServiceTest` | Profit and loss replay from trades |
| `ValuationServiceTest`, `ValuationControllerTest` | Valuation, unavailable reasons and totals policy |
| `PortfolioControllerSecurityTest`, `PerformanceControllerSecurityTest` | 401 without a valid token, user id taken from the token, POSTs from the deployed site not rejected |
| `RepositoryAdaptersTest`, `JpaPortfolioRepositoryTest`, `JpaHoldingRepositoryTest`, `JpaTradeRepositoryTest` | Database mapping and queries, including holding currency and trade order |
| `MarketDataPriceClientTest`, `FakePriceClientTest` | Price clients and their failure handling |

Frontend tests are in `frontend/tests` (`npm test` in `frontend`): `portfolioApi.test.js` covers the API client, and `portfolioSummary.test.js` covers the dashboard summary's totals, realised profit and allocation.

## Demonstration checklist

With users-service, market-data-service (stub profile), portfolio-service and the frontend running:

1. **Create a portfolio.** Register a new user and open Portfolio. The page offers "Create portfolio". Create one; its name appears in the card.
2. **Buy at market price.** Choose **Add shares → Buy now at market price**, enter `AAPL` and `2`, and confirm. The holding appears with a current value, and the totals fill in.
3. **Buy from a lookup.** Search `TSLA` in Stock lookup and press **Buy this stock**. The form opens with TSLA and an estimated total.
4. **Record an earlier purchase.** Choose **Record an earlier purchase**, enter `AAPL`, `3`, a price of `150`, and yesterday's date. The average price paid changes, and the trade appears in Trade history at yesterday's date.
5. **Reject a future date.** Try a date tomorrow. A clear error appears and nothing is saved.
6. **Sell.** Press **Sell** on AAPL and sell `1`. Trade history shows the sale.
7. **Block a backdated purchase before a sale.** Record an AAPL purchase dated before that sale. The error explains it must be after your most recent sale.
8. **Block selling too much.** Try to sell more AAPL than you hold. The error comes from the server and nothing changes.
9. **Filter the history.** Use Trade history's Stock and Type filters, then **Clear filters**.
10. **Dashboard summary.** Open the Dashboard. Below the transaction cards, Portfolio Value, Unrealised Gain / Loss and Realised Profit match the Portfolio page, and "Allocation by stock" shows each holding's share of the value.
11. **Market data down.** Stop market-data-service and refresh the Portfolio page. Holdings show "Current quote unavailable" and the totals are hidden rather than shown as zero. Buying shows a clear error. On the Dashboard, the portfolio cards show "Data Unavailable" while the transaction cards still load.
