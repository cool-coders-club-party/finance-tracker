package ie.universityofgalway.finance.portfolio.domain.model;

import java.math.BigDecimal;

public record HoldingValuation(Holding holding, MarketQuote quote, BigDecimal costBasis,
                               BigDecimal currentValue, BigDecimal unrealisedGainLoss,
                               String unavailableReason) {
    public boolean available() { return unavailableReason == null; }
}
