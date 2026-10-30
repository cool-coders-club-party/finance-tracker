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

