# Market Data API Contract

**Owner:** Jakub Zeman  
**Service:** `market-data-service`  
**Local URL:** `http://localhost:8084`

## Get a stock quote

```http
GET /api/quotes/{symbol}
```

The symbol is trimmed, case-insensitive and returned in uppercase.

Example:

```http
GET /api/quotes/aapl
```

### Success response

```http
200 OK
```

```json
{
  "symbol": "AAPL",
  "price": 348.91,
  "asOf": "2026-10-29T13:30:00Z",
  "stale": false,
  "details": {
    "name": "Apple Inc.",
    "currency": "USD",
    "open": 348.00,
    "high": 351.43,
    "low": 347.30,
    "volume": 42382900,
    "previousClose": 343.83,
    "change": 5.08,
    "percentChange": 1.48,
    "fiftyTwoWeekLow": 243.42,
    "fiftyTwoWeekHigh": 351.73
  }
}
```

- `symbol` — normalised ticker symbol.
- `price` — latest available share price.
- `asOf` — the provider's quote timestamp
- `stale` — `false` for a live or fresh cached quote; `true` when the provider fails and an older cached quote is returned. Keep `asOf` visible so users can judge the price's age.
- `details` - contains optional quote statistics and may be `null` in stub mode.

### Unknown symbol

```http
404 Not Found
```

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "No quote found for symbol: NOPE"
}
```

### Provider unavailable — `503 Service Unavailable`

Returned when the external provider fails and no cached quote is available.

```json
{
  "title": "Service Unavailable",
  "status": 503,
  "detail": "Market data is temporarily unavailable"
}
```

## Current implementation

The default `stub` profile uses fixed development prices for `AAPL`, `MSFT` and `TSLA`. The `local` profile uses Twelve Data for live prices. Successful quotes are cached per symbol for 15 minutes (configurable with `market-data.cache.freshness`). If the provider fails after a quote has expired, the latest cached quote is returned with `stale: true`.

## Service responsibilities

The market-data service supplies a price and timestamp.

The portfolio service owns holdings, quantities, market value and profit/loss calculations.

```text
market value = quantity × quote price
```

---

## Run locally

Requirements: Java 21 and installed frontend dependencies.

Keep the backend and frontend running in separate terminals. Start each terminal from the repository root.

### Backend: development prices

```bash
# Enter the backend folder.
cd backend

# Use fixed prices without contacting Twelve Data.
./mvnw -pl services/market-data-service spring-boot:run \
  -Dspring-boot.run.profiles=stub
```

The service runs on `http://localhost:8084`. Stub symbols include `AAPL`, `MSFT` and `TSLA`.

### Frontend

```bash
# Enter the frontend folder from the repository root.
cd frontend

# Start the development server.
npm run dev
```

Open the printed frontend URL and navigate to `/market-data`.

The Vite development proxy forwards `/market-data/api` requests to the backend on port 8084.

### Backend: Twelve Data prices

Configure the following properties in the ignored local configuration file at:

`backend/services/market-data-service/src/main/resources/application-local.yml`

- `market-data.twelve-data.base-url`: `https://api.twelvedata.com`
- `market-data.twelve-data.api-key`: your private Twelve Data key

Keep the key out of frontend files, commits, screenshots and issue descriptions.

Stop the stub backend before starting the live provider. From the backend folder:

```bash
# Activate the Twelve Data implementation and local configuration.
./mvnw -pl services/market-data-service spring-boot:run \
  -Dspring-boot.run.profiles=local
```

## Price history

`GET /api/quotes/{symbol}/history` returns approximately one calendar month of daily closing prices.

The response contains a `points` array with `date` and `close` fields, ordered oldest first.

History has no user-selectable date parameter yet. A history failure does not hide a successful quote on the frontend.

## Demonstration checklist

### Quote lookup and retry

With the stub backend running:

1. Search for ` aapl `, including surrounding spaces. The page displays an AAPL quote.
2. Search for `NOPE`. The page displays a clear unknown-ticker message.
3. Search for `MSFT`. A quote appears without refreshing the page.
4. Stop the backend and search again. The page reports that the service cannot be reached.
5. Restart the backend and press **Find price** again. The quote appears without refreshing the page.

### Provider unavailable: HTTP 503

Stop the backend, then run this command from the backend folder:

```bash
# Keep the backend available but deliberately make its provider unreachable.
# These temporary arguments do not change the saved configuration.
./mvnw -pl services/market-data-service spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.arguments="--market-data.twelve-data.base-url=http://127.0.0.1:1 --market-data.twelve-data.api-key=test-key"
```

Search for `AAPL`. With no cached quote, the backend returns HTTP 503 and the page displays:

“Market data is temporarily unavailable. Try again shortly.”

Confirm the status in the browser’s Network panel. Afterwards, stop this backend and restart it using the stub command.

### Stale cached fallback

From the backend folder:

```bash
# Run deterministic cache tests without contacting Twelve Data.
./mvnw -pl services/market-data-service \
  -Dtest=CachingMarketDataProviderTest test
```

The `returnsStaleCachedQuoteWhenProviderFails` test:

1. Caches a successful quote.
2. Advances the simulated time beyond the freshness period.
3. Simulates a provider failure.
4. Checks that the fallback preserves the price, original timestamp and quote details, with `stale: true`.

When the frontend receives a stale quote, it displays the cached-price warning and the quote timestamp.

The cache is held in memory and is cleared when the backend restarts. Restarting with a broken provider therefore demonstrates 503, rather than stale fallback.

