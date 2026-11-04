// Both Portfolio endpoints return arrays.
async function getPortfolioList(path, signal) {
  let response;

  try {
    response = await fetch(`/portfolio-api/api/portfolios${path}`, {
      signal,
    });
  } catch (error) {
    // Preserve cancellation when the user leaves the page.
    if (error.name === "AbortError") throw error;

    throw new Error("Cannot reach Portfolio. Try again shortly.");
  }

  if (!response.ok) {
    throw new Error(
      response.status === 502 || response.status === 504
        ? "Cannot reach Portfolio. Try again shortly."
        : `Portfolio request failed (${response.status}).`
    );
  }

  const data = await response.json();

  // Reject an unexpected response instead of crashing while rendering rows.
  if (!Array.isArray(data)) {
    throw new Error("Portfolio returned an unexpected response.");
  }

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