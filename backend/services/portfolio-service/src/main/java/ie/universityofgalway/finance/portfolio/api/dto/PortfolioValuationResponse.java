package ie.universityofgalway.finance.portfolio.api.dto;

import ie.universityofgalway.finance.portfolio.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Public portfolio valuation response; incomplete results omit all aggregate money totals.
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
public record PortfolioValuationResponse(List<ValuedHoldingResponse> holdings, String currency,
        boolean complete, boolean stale, int unavailableHoldings, BigDecimal costBasis,
        BigDecimal currentValue, BigDecimal unrealisedGainLoss) {

    /**
     * A saved holding with its market snapshot and valuation availability.
     *
     * @param id holding identifier
     * @param ticker stock symbol
     * @param quantity shares held
     * @param avgPurchasePrice saved weighted average purchase price
     * @param currency saved purchase currency; may be null
     * @param quote market snapshot; may be null
     * @param valuationAvailable whether the holding has usable price and currency metadata
     * @param unavailableReason reason code, or null when available
     * @param costBasis quantity times average purchase price, or null if unavailable
     * @param currentValue quantity times market price, or null if unavailable
     * @param unrealisedGainLoss current value minus cost basis, or null if unavailable
     */
    public record ValuedHoldingResponse(Long id, String ticker, BigDecimal quantity,
            BigDecimal avgPurchasePrice, String currency, MarketQuote quote, boolean valuationAvailable,
            String unavailableReason, BigDecimal costBasis, BigDecimal currentValue,
            BigDecimal unrealisedGainLoss) {
        /**
         * Maps a holding valuation without inventing missing prices or currencies.
         *
         * @param value holding valuation result
         * @return the public holding valuation
         */
        static ValuedHoldingResponse from(HoldingValuation value) {
            Holding h = value.holding();
            return new ValuedHoldingResponse(h.getId(), h.getTicker(), h.getQuantity(),
                    h.getAvgPurchasePrice(), h.getCurrency(), value.quote(), value.available(),
                    value.unavailableReason(), value.costBasis(), value.currentValue(), value.unrealisedGainLoss());
        }
    }

    /**
     * Maps per-holding results and aggregate availability into the API response.
     *
     * @param value portfolio valuation result
     * @return the public portfolio valuation
     */
    public static PortfolioValuationResponse from(PortfolioValuation value) {
        return new PortfolioValuationResponse(value.holdings().stream().map(ValuedHoldingResponse::from).toList(),
                value.currency(), value.complete(), value.stale(), value.unavailableHoldings(),
                value.costBasis(), value.currentValue(), value.unrealisedGainLoss());
    }
}
