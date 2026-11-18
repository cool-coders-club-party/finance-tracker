import { authenticatedFetch } from "../lib/authSession.js";

const BASE = "/portfolio-api/api/portfolios";

// One place for transport and error handling, shared by reads and writes.
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

async function getList(path, signal) {
  const data = await request(path, { signal });
  // Reject an unexpected response instead of crashing while rendering rows.
  if (!Array.isArray(data)) throw new Error("Portfolio returned an unexpected response.");
  return data;
}

const portfolioPath = (portfolioId, suffix) => `/${encodeURIComponent(portfolioId)}/${suffix}`;

// --- Reads ---

// Discover the available portfolios instead of hard-coding portfolio ID 1.
export function getPortfolios(signal) {
  return getList("", signal);
}

// Request the holdings belonging to one portfolio.
export function getHoldings(portfolioId, signal) {
  return getList(portfolioPath(portfolioId, "holdings"), signal);
}

// Every buy and sell recorded against the portfolio.
export function getTrades(portfolioId, signal) {
  return getList(portfolioPath(portfolioId, "trades"), signal);
}

export async function getValuation(portfolioId, signal) {
  const data = await request(portfolioPath(portfolioId, "valuation"), { signal });
  if (!data || !Array.isArray(data.holdings) || typeof data.complete !== "boolean") {
    throw new Error("Portfolio returned an unexpected valuation response.");
  }
  return data;
}

// --- Writes ---

export function createPortfolio(name) {
  return request("", { method: "POST", body: { name } });
}

// Without price/executedAt the backend buys at the current market price.
// With both, it records a purchase made earlier at the price actually paid.
export function buy(portfolioId, ticker, quantity, { price, executedAt } = {}) {
  const body = { ticker, quantity };
  if (price !== undefined) body.price = price;
  if (executedAt !== undefined) body.executedAt = executedAt;

  return request(portfolioPath(portfolioId, "buy"), { method: "POST", body });
}

export function sell(portfolioId, ticker, quantity) {
  return request(portfolioPath(portfolioId, "sell"), {
    method: "POST",
    body: { ticker, quantity },
  });
}
