/**
 * Market Data requests use the configured deployment URL or the development proxy.
 * Only ranking requests currently accept an AbortSignal; quote and history requests
 * run to completion once started.
 */
const BASE_URL = (
  import.meta.env?.VITE_MARKET_DATA_API_BASE_URL || "/market-data"
).replace(/\/$/, "");

/**
 * Fetches the manually maintained top-five snapshot and checks its row count and capture date.
 * The capture date is descriptive: an old snapshot does not expire automatically.
 *
 * @param {AbortSignal} [signal] cancels the request when the caller leaves the page.
 * @return {Promise<object>} The snapshot with capturedOn, sourceUrl and exactly five rows.
 * @throws {Error} if the request fails or the snapshot structure is invalid; cancellation rejects with AbortError.
 */
export async function getTopStocks(signal) {
  const response = await fetch(`${BASE_URL}/api/rankings/top-stocks`, { signal });
  if (!response.ok) {
    throw new Error("Ranking unavailable");
  }

  const snapshot = await response.json();
  if (!Array.isArray(snapshot?.rows) || snapshot.rows.length !== 5 ||
      !Number.isFinite(Date.parse(snapshot.capturedOn))) {
    throw new Error("Invalid top stocks snapshot");
  }

  return snapshot;
}

/**
 * Fetches the watchlist movement ranking and validates coverage counts, changes and timestamps.
 *
 * @param {AbortSignal} [signal] cancels the request when the caller leaves the page.
 * @return {Promise<object>} Coverage counts and up to five rows; changes are percentage points (8 means 8%).
 * @throws {Error} if the request fails or the response is invalid; cancellation rejects with AbortError.
 */
export async function getStockMovers(signal) {
  const response = await fetch(`${BASE_URL}/api/rankings/movers`, { signal });
  if (!response.ok) {
    throw new Error("Stock movers are temporarily unavailable");
  }

  const ranking = await response.json();
  if (
    !Number.isInteger(ranking?.trackedCount) ||
    !Number.isInteger(ranking.usableCount) ||
    !Array.isArray(ranking.rows) ||
    !ranking.rows.every((row) =>
      row && Number.isFinite(row.percentChange) && Number.isFinite(Date.parse(row.asOf))
    )
  ) {
    throw new Error("Invalid stock movers ranking");
  }

  return ranking;
}

/**
 * Fetches a quote, retaining the provider timestamp, stale flag and optional details.
 * HTTP 404, service failures and transport failures become readable error messages.
 *
 * @param {string} symbol ticker encoded as one URL path segment.
 * @return {Promise<object>} The quote JSON, or an empty object if the success body is not valid JSON.
 * @throws {Error} if the lookup fails.
 */
export async function getQuote(symbol) {
  let response;

  try {
    response = await fetch(
      `${BASE_URL}/api/quotes/${encodeURIComponent(symbol)}`
    );
  } catch {
    // Fetch failed before the server returned a response.
    throw new Error("Cannot reach the Market Data service. Try again shortly.");
  }

  const body = await response.json().catch(() => ({}));

  if (response.status === 404) {
    throw new Error(`No quote found for ${symbol}. Check the ticker and try again.`);
  }

  if (response.status === 503) {
    throw new Error("Market data is temporarily unavailable. Try again shortly.");
  }

  if (response.status === 502 || response.status === 504) {
    throw new Error("Cannot reach the Market Data service. Try again shortly.");
  }

  if (!response.ok) {
    throw new Error(body.detail || `Quote request failed (${response.status}).`);
  }

  return body;
}

/**
 * Fetches the daily closing-price series used by the stock chart.
 *
 * @param {string} symbol ticker encoded as one URL path segment.
 * @return {Promise<Array<{date: string, close: number|string}>>} Daily points in service order, oldest first.
 * @throws {Error} if the lookup fails or the response does not contain a points array.
 */
export async function getPriceHistory(symbol) {
  let response;

  try {
    response = await fetch(
      `${BASE_URL}/api/quotes/${encodeURIComponent(symbol)}/history`
    );
  } catch {
    throw new Error("Cannot reach the Market Data service for price history.");
  }

  const body = await response.json().catch(() => ({}));

  if (response.status === 404) {
    throw new Error(`No recent price history is available for ${symbol}.`);
  }

  if (response.status === 503) {
    throw new Error("Price history is temporarily unavailable.");
  }

  if (!response.ok) {
    throw new Error(body.detail || `History request failed (${response.status}).`);
  }

  // The graph expects an array of date-and-price points.
  if (!Array.isArray(body.points)) {
    throw new Error("The price history response is invalid.");
  }

  return body.points;
}
