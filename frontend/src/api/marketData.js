const BASE_URL = (
  import.meta.env.VITE_MARKET_DATA_API_BASE_URL || "/market-data"
).replace(/\/$/, "");

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