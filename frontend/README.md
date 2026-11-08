# Finance Tracker frontend

```sh
npm ci
npm run dev
```

Open the URL printed by Vite (normally http://localhost:5173).

- `/dashboard`: existing dashboard and market-data integration.
- `/transactions`: create a transaction using metadata-provided types/categories.
- `/transactions/:transactionId/edit`: load and edit an existing transaction.

Run PostgreSQL and Transactions on port 8082 for transaction forms. Vite proxies
`/api/v1/transactions` to that service; market-data requests use port 8084.
Creation currently requires a user ID until authentication is integrated.

Metadata is loaded once per page mount (React Strict Mode may cancel and repeat
the initial development request). Changing types filters locally; valid shared
categories remain selected, while incompatible selections clear. API failures
offer retry actions. Backend validation errors appear without clearing the form.
Amounts are submitted as decimal strings to avoid floating-point conversion.
Creation automatically sends the current local date/time at submission, following
the backend's current local date-time contract. Date/time is not editable in the
form; updates leave the stored timestamp unchanged.

## Checks

```sh
npm test
npm run build
```

The tests cover metadata compatibility, future enum options, malformed responses,
request cancellation, decimal/enum payloads, and backend validation errors.

Manual checks:

1. Select Income / Salary, then switch to Expense: Salary clears.
2. Select Gift and switch between types: Gift remains selected.
3. Load an edit URL: the saved type/category remain selected.
4. Save a create/edit form: raw enum values are sent; edits send changed fields only.
5. With Transactions unavailable, retry loading options after restarting it.
