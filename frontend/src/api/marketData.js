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