package ie.universityofgalway.finance.portfolio.api.dto;

import ie.universityofgalway.finance.portfolio.domain.model.Holding;

import java.math.BigDecimal;

/**
 * Saved holding data, separate from any live market valuation.
 *
 * @param id holding identifier
 * @param ticker stock symbol
 * @param quantity shares currently held
 * @param avgPurchasePrice weighted average purchase price per share in the saved currency
 * @param currency saved purchase currency; null for legacy holdings with unknown currency
 */
public record HoldingResponse(
        Long id,
        String ticker,
        BigDecimal quantity,
        BigDecimal avgPurchasePrice,
        String currency
) {
    /**
     * Maps a domain holding without substituting a current market price for its purchase cost.
     *
     * @param holding saved domain holding
     * @return the public holding representation
     */
    public static HoldingResponse from(Holding holding) {
        return new HoldingResponse(
                holding.getId(),
                holding.getTicker(),
                holding.getQuantity(),
                holding.getAvgPurchasePrice(),
                holding.getCurrency()
        );
    }
}
