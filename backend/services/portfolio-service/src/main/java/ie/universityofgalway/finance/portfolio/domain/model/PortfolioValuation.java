package ie.universityofgalway.finance.portfolio.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Per-holding market values and aggregate availability for one portfolio.
 *
 * <p>The valuation service sets all aggregate amounts to null if any holding is unavailable.</p>
 *
 * @param holdings per-holding results, including unavailable valuations
 * @param currency currency of aggregate amounts; currently USD
 * @param complete whether every holding can be valued
 * @param stale whether any attached quote is a cached fallback
 * @param unavailableHoldings number of holdings that cannot be valued
 * @param costBasis aggregate cost of current holdings, or null if incomplete
 * @param currentValue aggregate market value, or null if incomplete
 * @param unrealisedGainLoss aggregate market value minus cost basis, or null if incomplete
 */
public record PortfolioValuation(List<HoldingValuation> holdings, String currency, boolean complete,
                                 boolean stale, int unavailableHoldings, BigDecimal costBasis,
                                 BigDecimal currentValue, BigDecimal unrealisedGainLoss) {
}
