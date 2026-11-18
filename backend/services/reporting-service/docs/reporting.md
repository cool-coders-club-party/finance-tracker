# Reporting CSV Export

## Endpoint

`GET /api/reports/export?from=YYYY-MM-DD&to=YYYY-MM-DD`

Requires the caller's bearer JWT. The date range is inclusive. Transactions are
sorted by occurrence date and time, oldest first. Holdings are current as of the
export and are not filtered by the transaction date range.

## File layout

The UTF-8 CSV contains a report heading with the user ID and date range, followed
by two sections:

- **Transactions:** `AMOUNT`, `CURRENCY`, `CATEGORY`, `DATE & TIME`
- **Investments:** `AMOUNT INVESTED`, `CURRENT PRICE`, `TICKER`

Transaction date-times use ISO local date-time format, for example
`2026-04-05T13:45:00`. Amounts use plain decimal notation. CSV cells are quoted;
quotes inside a cell are doubled. Empty sections still include their section
label and column header.

The investment amount invested is the portfolio cost basis. Investment rows
include only holdings with a positive quantity. The Portfolio response does not
currently provide an explicit currency code for investment values.