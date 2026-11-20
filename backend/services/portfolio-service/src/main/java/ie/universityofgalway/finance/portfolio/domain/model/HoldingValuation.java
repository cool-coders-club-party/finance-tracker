package ie.universityofgalway.finance.portfolio.domain.model;

import java.math.BigDecimal;

/**
 * A holding's market valuation or the reason why it cannot be valued.
 *
 * @param holding saved holding whose purchase cost remains unchanged
 * @param quote market snapshot; may be null when unavailable
 * @param costBasis quantity times average purchase price, or null if unavailable
 * @param currentValue quantity times market price, or null if unavailable
 * @param unrealisedGainLoss current value minus cost basis, or null if unavailable
 * @param unavailableReason reason code when unavailable, or null for an available valuation
 */
public record HoldingValuation(Holding holding, MarketQuote quote, BigDecimal costBasis,
                               BigDecimal currentValue, BigDecimal unrealisedGainLoss,
                               String unavailableReason) {
    /**
     * Determines availability from the absence of a reason code.
     *
     * @return true when {@code unavailableReason} is null
     */
    public boolean available() { return unavailableReason == null; }
}
