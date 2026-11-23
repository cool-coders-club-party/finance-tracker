/**
 * Pure calculations for the dashboard's portfolio summary, kept out of the component so they
 * can be unit-tested without rendering.
 */

/**
 * Converts a JSON number (or numeric string) to a finite number, or null.
 *
 * @param {*} value backend value, possibly null
 * @return {number|null} The number, or null when missing or not finite.
 */
function toNumber(value) {
  if (value == null) return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

/**
 * Combines the valuation and performance responses into the figures the dashboard shows.
 *
 * Totals follow the backend's completeness policy: when any holding can't be valued, the
 * valuation totals are null and stay null here, rather than being recalculated from a subset.
 * Allocation only uses holdings that could be valued, and reports how many were left out.
 *
 * @param {object} valuation response from GET /api/portfolios/{id}/valuation
 * @param {Array|null} performance response from GET /api/portfolios/{id}/performance,
 *        or null when it couldn't be loaded
 * @return {{
 *   currency: string,
 *   currentValue: number|null,
 *   costBasis: number|null,
 *   unrealised: number|null,
 *   unrealisedPercent: number|null,
 *   realised: number|null,
 *   complete: boolean,
 *   stale: boolean,
 *   unavailableHoldings: number,
 *   allocation: Array<{ticker: string, value: number, share: number}>,
 *   excludedFromAllocation: number
 * }} The dashboard figures; null means "show as unavailable".
 */
export function summarisePortfolio(valuation, performance) {
  const costBasis = toNumber(valuation.costBasis);
  const unrealised = toNumber(valuation.unrealisedGainLoss);

  // Fully sold holdings stay in the database with zero shares; they hold no value.
  const held = valuation.holdings.filter((holding) => toNumber(holding.quantity) > 0);
  const valued = held
    .filter((holding) => holding.valuationAvailable && toNumber(holding.currentValue) != null)
    .map((holding) => ({ ticker: holding.ticker, value: toNumber(holding.currentValue) }));
  const allocationTotal = valued.reduce((sum, holding) => sum + holding.value, 0);

  return {
    currency: valuation.currency || 'USD',
    currentValue: toNumber(valuation.currentValue),
    costBasis,
    unrealised,
    unrealisedPercent: unrealised != null && costBasis > 0 ? (unrealised / costBasis) * 100 : null,
    // Realised profit includes closed positions, which only the performance endpoint reports.
    realised: Array.isArray(performance)
      ? performance.reduce((sum, item) => sum + (toNumber(item.realizedPnl) ?? 0), 0)
      : null,
    complete: valuation.complete,
    stale: Boolean(valuation.stale),
    unavailableHoldings: valuation.unavailableHoldings ?? 0,
    allocation: valued
      .map((holding) => ({
        ...holding,
        share: allocationTotal > 0 ? (holding.value / allocationTotal) * 100 : 0,
      }))
      .sort((a, b) => b.value - a.value),
    excludedFromAllocation: held.length - valued.length,
  };
}
