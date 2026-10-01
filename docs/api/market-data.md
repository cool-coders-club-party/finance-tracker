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
  "asOf": "2026-10-01T12:00:00Z"
}
```

- `symbol` — normalised ticker symbol.
- `price` — latest available share price.
- `asOf` — time the price was obtained.

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

## Current implementation

The service currently uses a stub provider with fixed prices for `AAPL`, `MSFT` and `TSLA`. These are development values, not live prices.

## Service responsibilities

The market-data service supplies a price and timestamp.

The portfolio service owns holdings, quantities, market value and profit/loss calculations.

```text
market value = quantity × quote price
```

## Planned changes

The following are not implemented yet:

- connection to a live market-data provider;
- quote currency;
- price caching;
- a `stale` indicator for cached prices;
- `503 Service Unavailable` when neither live nor cached data is available.

Contract changes must be agreed with affected team members before implementation.nt services are updated.