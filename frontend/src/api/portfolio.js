import { authenticatedFetch } from "../lib/authSession.js";

// Keep transport errors consistent for the list and valuation endpoints.
async function getPortfolioData(path, signal) {
  let response;

  try {
    response = await authenticatedFetch(`/portfolio-api/api/portfolios${path}`, {
      signal,
    });
  } catch (error) {
    // Preserve cancellation when the user leaves the page.
    if (error.name === "AbortError") throw error;

    throw new Error("Cannot reach Portfolio. Try again shortly.", { cause: error });
  }

  if (!response.ok) {
    throw new Error(
      response.status === 502 || response.status === 504
        ? "Cannot reach Portfolio. Try again shortly."
        : `Portfolio request failed (${response.status}).`
    );
  }

  const data = await response.json();

  return data;
}

// Discover the available portfolios instead of hard-coding portfolio ID 1.
export function getPortfolios(signal) {
  return getPortfolioList("", signal);
}

// Request the holdings belonging to one portfolio.
export function getHoldings(portfolioId, signal) {
  return getPortfolioList(
    `/${encodeURIComponent(portfolioId)}/holdings`,
    signal
  );
}

async function getPortfolioList(path, signal) {
  const data = await getPortfolioData(path, signal);
  if (!Array.isArray(data)) throw new Error("Portfolio returned an unexpected response.");
  return data;
}

export async function getValuation(portfolioId, signal) {
  const data = await getPortfolioData(`/${encodeURIComponent(portfolioId)}/valuation`, signal);
  if (!data || !Array.isArray(data.holdings) || typeof data.complete !== "boolean") {
    throw new Error("Portfolio returned an unexpected valuation response.");
  }
  return data;
}
