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
  "price": 192.45,
  "asOf": "2026-10-01T12:00:00Z",
  "stale": false
}
```

- `symbol` — normalised ticker symbol.
- `price` — latest available share price.
- `asOf` — time the price was obtained.
- `stale` — `false` for a live or fresh cached quote; `true` when the provider fails and an older cached quote is returned. Keep `asOf` visible so users can judge the price's age.

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

## Planned changes

The following are not implemented yet:

- quote currency;

Contract changes must be agreed with affected team members before implementation.
