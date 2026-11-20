import { authenticatedFetch } from "../lib/authSession.js";

const BASE = "/portfolio-api/api/portfolios";

/**
 * Sends an authenticated Portfolio request and parses its optional JSON body.
 * Preserves AbortError and attaches backend status/code to HTTP errors. Session
 * restoration and expiry checks are handled by authenticatedFetch.
 *
 * @param {string} path path relative to the portfolios endpoint.
 * @param {object} [options] request settings.
 * @param {string} [options.method="GET"] HTTP method.
 * @param {object} [options.body] payload to serialise as JSON.
 * @param {AbortSignal} [options.signal] optional cancellation signal.
 * @return {Promise<object|Array|null>} Parsed JSON, or null for an empty response body.
 * @throws {Error} if transport, authentication, HTTP status or JSON parsing fails.
 */
async function request(path, { method = "GET", body, signal } = {}) {
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";

  let response;
  try {
    response = await authenticatedFetch(`${BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (error) {
    // Preserve cancellation when the user leaves the page.
    if (error.name === "AbortError") throw error;

    throw new Error("Cannot reach Portfolio. Try again shortly.", { cause: error });
  }

  if (!response.ok) {
    if (response.status === 502 || response.status === 504) {
      throw new Error("Cannot reach Portfolio. Try again shortly.");
    }

    // GlobalExceptionHandler returns { error, message }, e.g. INSUFFICIENT_SHARES.
    let apiError = null;
    try {
      apiError = await response.json();
    } catch {
      // Empty or non-JSON body.
    }

    const error = new Error(
      apiError?.message || `Portfolio request failed (${response.status}).`
    );
    error.status = response.status;
    error.code = apiError?.error;
    throw error;
  }

  // Don't assume a body: response.json() throws on an empty one.
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

/**
 * Requests a list endpoint and rejects unexpected response shapes.
 *
 * @param {string} path relative endpoint path.
 * @param {AbortSignal} [signal] optional cancellation signal.
 * @return {Promise<Array>} The returned list.
 * @throws {Error} if the request fails or the response is not an array.
 */
async function getList(path, signal) {
  const data = await request(path, { signal });
  // Reject an unexpected response instead of crashing while rendering rows.
  if (!Array.isArray(data)) throw new Error("Portfolio returned an unexpected response.");
  return data;
}

/**
 * Builds a portfolio resource path with the identifier encoded as one path segment.
 *
 * @param {number|string} portfolioId portfolio identifier.
 * @param {string} suffix internal endpoint name, such as holdings or valuation.
 * @return {string} The relative resource path.
 */
const portfolioPath = (portfolioId, suffix) => `/${encodeURIComponent(portfolioId)}/${suffix}`;

// --- Reads ---

/**
 * Lists the authenticated user's portfolios without assuming a fixed identifier.
 *
 * @param {AbortSignal} [signal] optional cancellation signal.
 * @return {Promise<Array>} The user's portfolios.
 * @throws {Error} if the request fails or the response is not an array.
 */
export function getPortfolios(signal) {
  return getList("", signal);
}

/**
 * Retrieves saved holdings, including their purchase costs for one portfolio.
 *
 * @param {number|string} portfolioId portfolio identifier.
 * @param {AbortSignal} [signal] optional cancellation signal.
 * @return {Promise<Array>} The holdings returned by the backend.
 * @throws {Error} if the request fails or the response is not an array.
 */
export function getHoldings(portfolioId, signal) {
  return getList(portfolioPath(portfolioId, "holdings"), signal);
}

/**
 * Retrieves recorded buy and sell trades for one portfolio.
 *
 * @param {number|string} portfolioId portfolio identifier.
 * @param {AbortSignal} [signal] optional cancellation signal.
 * @return {Promise<Array>} The trades returned by the backend.
 * @throws {Error} if the request fails or the response is not an array.
 */
export function getTrades(portfolioId, signal) {
  return getList(portfolioPath(portfolioId, "trades"), signal);
}

/**
 * Retrieves saved holdings alongside market quotes and valuation availability.
 * Null totals, missing currencies and stale-price metadata are preserved for honest display.
 * Validation checks the holdings array and complete flag rather than every nested field.
 *
 * @param {number|string} portfolioId portfolio identifier.
 * @param {AbortSignal} [signal] optional cancellation signal.
 * @return {Promise<object>} Holding valuations and portfolio totals, which may be null when incomplete.
 * @throws {Error} if the request fails or the expected response structure is absent.
 */
export async function getValuation(portfolioId, signal) {
  const data = await request(portfolioPath(portfolioId, "valuation"), { signal });
  if (!data || !Array.isArray(data.holdings) || typeof data.complete !== "boolean") {
    throw new Error("Portfolio returned an unexpected valuation response.");
  }
  return data;
}

// --- Writes ---

/**
 * Creates a portfolio for the authenticated user.
 *
 * @param {string} name portfolio display name; validation is enforced by the backend.
 * @return {Promise<object|null>} The created portfolio response.
 * @throws {Error} if the backend rejects the request or the service is unreachable.
 */
export function createPortfolio(name) {
  return request("", { method: "POST", body: { name } });
}

/**
 * Buys shares at the market price, or records an earlier purchase when price and executedAt are supplied.
 * Both historical fields must be supplied together; the backend enforces this rule.
 *
 * @param {number|string} portfolioId portfolio receiving the shares.
 * @param {string} ticker stock symbol.
 * @param {number} quantity positive share quantity.
 * @param {object} [options] optional historical execution details.
 * @param {number} [options.price] price per share actually paid.
 * @param {string} [options.executedAt] ISO timestamp of the earlier purchase.
 * @return {Promise<object|null>} The backend purchase result.
 * @throws {Error} if purchase validation, ownership checks or the request fails.
 */
export function buy(portfolioId, ticker, quantity, { price, executedAt } = {}) {
  const body = { ticker, quantity };
  if (price !== undefined) body.price = price;
  if (executedAt !== undefined) body.executedAt = executedAt;

  return request(portfolioPath(portfolioId, "buy"), { method: "POST", body });
}

/**
 * Sells shares from the portfolio at the price obtained by the backend.
 *
 * @param {number|string} portfolioId portfolio containing the holding.
 * @param {string} ticker stock symbol.
 * @param {number} quantity positive share quantity, no greater than the holding.
 * @return {Promise<object|null>} The backend sale result.
 * @throws {Error} if validation, ownership, available shares or the request prevents the sale.
 */
export function sell(portfolioId, ticker, quantity) {
  return request(portfolioPath(portfolioId, "sell"), {
    method: "POST",
    body: { ticker, quantity },
  });
}
