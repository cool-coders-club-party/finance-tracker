package ie.universityofgalway.finance.portfolio.api.dto;

import ie.universityofgalway.finance.portfolio.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

public record PortfolioValuationResponse(List<ValuedHoldingResponse> holdings, String currency,
        boolean complete, boolean stale, int unavailableHoldings, BigDecimal costBasis,
        BigDecimal currentValue, BigDecimal unrealisedGainLoss) {

    public record ValuedHoldingResponse(Long id, String ticker, BigDecimal quantity,
            BigDecimal avgPurchasePrice, String currency, MarketQuote quote, boolean valuationAvailable,
            String unavailableReason, BigDecimal costBasis, BigDecimal currentValue,
            BigDecimal unrealisedGainLoss) {
        static ValuedHoldingResponse from(HoldingValuation value) {
            Holding h = value.holding();
            return new ValuedHoldingResponse(h.getId(), h.getTicker(), h.getQuantity(),
                    h.getAvgPurchasePrice(), h.getCurrency(), value.quote(), value.available(),
                    value.unavailableReason(), value.costBasis(), value.currentValue(), value.unrealisedGainLoss());
        }
    }

    public static PortfolioValuationResponse from(PortfolioValuation value) {
        return new PortfolioValuationResponse(value.holdings().stream().map(ValuedHoldingResponse::from).toList(),
                value.currency(), value.complete(), value.stale(), value.unavailableHoldings(),
                value.costBasis(), value.currentValue(), value.unrealisedGainLoss());
    }
}
