const BASE_URL = (
  import.meta.env.VITE_MARKET_DATA_API_BASE_URL || "/market-data"
).replace(/\/$/, "");

export async function getQuote(symbol) {
  const response = await fetch(
    `${BASE_URL}/api/quotes/${encodeURIComponent(symbol)}`
  );

  const body = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(body.detail || `Quote request failed (${response.status}).`);
  }

  return body;
}