# Portfolio valuation and purchase integration

Issue: #101. Related work assigned to `Reddragon9753`: #100 (purchase persistence) and #102 (purchase form).

## Read contract

`GET /api/portfolios/{portfolioId}/valuation` requires the signed-in user's bearer token. It returns 401 without authentication and 404 for a missing portfolio or a portfolio owned by someone else. Quote requests only happen after ownership has been checked.

The frontend calls this through `/portfolio-api/api/portfolios/{portfolioId}/valuation`. Existing `/holdings` and `/trades` endpoints retain their list response shapes. `/holdings` and buy/sell responses now additionally expose the saved holding `currency` (nullable).

Example complete valuation:

```json
{
  "holdings": [
    {
      "id": 1,
      "ticker": "AAPL",
      "quantity": 2,
      "avgPurchasePrice": 100,
      "currency": "USD",
      "quote": {
        "symbol": "AAPL",
        "price": 125.5,
        "currency": "USD",
        "asOf": "2026-11-16T12:00:00Z",
        "stale": false
      },
      "valuationAvailable": true,
      "unavailableReason": null,
      "costBasis": 200,
      "currentValue": 251,
      "unrealisedGainLoss": 51
    }
  ],
  "currency": "USD",
  "complete": true,
  "stale": false,
  "unavailableHoldings": 0,
  "costBasis": 200,
  "currentValue": 251,
  "unrealisedGainLoss": 51
}
```

Calculations use `BigDecimal`, without rounding intermediate values:

- `costBasis = quantity × saved avgPurchasePrice` (cost of shares still held, not lifetime spending).
- `currentValue = quantity × quote.price`.
- `unrealisedGainLoss = currentValue − costBasis`.
- Complete portfolio totals sum those holding values. The frontend rounds only for display.

The quote is a read-only snapshot. Reading valuations never saves a quote as a purchase price or modifies a holding or trade. `asOf` is the provider's timestamp, not the request time or purchase time. A stale quote is usable if its metadata and currency are valid; its original timestamp and stale warning remain visible.

## Unavailable data and totals

Pricing failures return HTTP 200 with the saved holding still present. Unavailable holdings have `valuationAvailable: false`, a reason below, and null `costBasis`, `currentValue` and `unrealisedGainLoss`. `quote` is null on a failed request; otherwise its returned metadata is preserved so the UI can explain the problem.

| Reason | Meaning |
| --- | --- |
| `QUOTE_UNAVAILABLE` | Unknown ticker, network failure, timeout, provider error or malformed/incomplete HTTP quote response. |
| `INVALID_QUOTE` | A PriceClient implementation supplied invalid price/symbol/timestamp metadata. |
| `CURRENCY_MISSING` | Purchase or quote currency is missing/blank. |
| `CURRENCY_UNSUPPORTED` | Purchase currency is not USD, or quote and purchase currencies differ. No FX conversion is performed. |

If any holding is unavailable, `complete` is false, `unavailableHoldings` counts it, and **all three portfolio totals are null**. Available holdings keep their individual values. Never sum just the available holdings and label that subtotal as the portfolio total. An empty portfolio is complete with zero totals. Summary `currency` identifies the supported valuation currency (USD); it does not fill in missing holding currencies. Summary `stale` is true if any returned quote is stale.

## Market Data configuration

Portfolio defaults to `price-client.type: market-data` and calls `GET /api/quotes/{ticker}` at `market-data.base-url` (default `http://localhost:8084`). Connect/read timeouts are 1000/2000 ms. The HTTP adapter reads currency from `details.currency` in the Market Data response, flattening it into `quote.currency` for Portfolio consumers.

Market Data's default stub profile may return `details: null`, so its prices are deliberately unavailable for valuation: missing currency must not be assumed to mean USD. Use the Market Data `local` profile with valid quote currency, or deterministic test responses. The old Portfolio `fake` client supplies bare prices only and does not supply valuation metadata.

## Handoff for Niall: #100

Merge/rebase onto the #101 changes before changing the same domain/entity/DTO files. This branch adds nullable `holdings.currency`, maps it through `HoldingRepositoryAdapter`, exposes `Holding.getCurrency()`, and adds `Holding.open(portfolioId, ticker, currency)` plus a constructor accepting currency. Use those rather than introducing another holding-currency field.

The existing Hibernate `ddl-auto: update` configuration adds the nullable column. Legacy rows stay null; do not backfill USD solely because the latest quote is USD. A legacy holding needs an explicit, verified purchase currency before valuation becomes available. Do not simply set a legacy holding to USD during a later buy and combine it with an unknown-currency cost basis; choose and document a safe policy (for example reject adding to it until its existing purchase data is corrected).

#100 still needs to implement the purchase write contract:

```http
POST /api/portfolios/{portfolioId}/buy
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "ticker": "AAPL",
  "quantity": 2,
  "pricePerShare": 100,
  "currency": "USD",
  "purchasedAt": "2026-11-16T12:00:00Z"
}
```

Validate all fields, positive decimal quantity/price, supported ticker, USD currency and a non-future offset/UTC timestamp. Use `BigDecimal` and `Instant`. Save the supplied purchase price/currency/time in trade history, and save holding quantity/weighted average/currency atomically. Preserve the existing JWT ownership checks. Purchase recording must work when Market Data is offline: remove the live-price lookup from the buy path. Sell recording is outside #100/#101 scope.

**Until #100 is complete, the current buy endpoint still uses its old ticker/quantity contract and quote-based purchase price. #101 does not make it ready for the new form's Save action.** Newly recorded or legacy holdings without saved currency correctly show unavailable valuation.

## Handoff for Niall: #102

The Portfolio page now loads `getValuation(portfolioId, signal)` and renders backend totals, row values, timestamps, cached warnings and unavailable reasons. Keep the existing abort handling and per-user result guards.

Connect the purchase form only after #100 supports all five purchase fields. On successful save, close/reset as appropriate and increment `portfolioReload` to fetch the updated valuation. Never overwrite `avgPurchasePrice` with `quote.price` in the UI, and do not recalculate portfolio totals from a partial set of rows.

Stock Lookup's Market Data response still uses `details.currency`; Portfolio's nested valuation quote uses `quote.currency`. For form prefill, the lookup price is only an editable estimate. Preserve the quote's `asOf` and stale warning separately. The actual `purchasedAt` must come from the user's purchase date/time, converted from their labelled local timezone to an ISO offset/UTC timestamp. Keep draft-replacement confirmation, validation, saving/error states and small-screen/keyboard access required by #102.

## Validation

From `backend`:

```bash
./mvnw -pl services/portfolio-service clean test
```

The tests cover HTTP quote mapping/failures/metadata, stale/fresh quotes, fractional quantities, gain/loss and totals, mixed availability, missing/mismatched currencies, unchanged purchase cost, authentication/ownership, JSON response fields and currency reload from persistence.

From `frontend`:

```bash
npm test
npm run build
npx eslint src/api/portfolio.js src/pages/Portfolio.jsx src/lib/valuation.js tests/portfolioApi.test.js tests/valuation.test.js
```

For a manual integration check after #100: record a USD purchase at 100 for two shares; a quote at 125.50 must show cost 200, current value 251, gain 51. Repeat with a stale quote (same values plus warning and original timestamp), then an unavailable quote (holding retained, row values/totals unavailable). A holding with unknown or incompatible currency must never show a guessed USD valuation.

Merge the reviewed #101 change with `Closes #101` in the PR description. Closing #101 does not close #100 or #102.
