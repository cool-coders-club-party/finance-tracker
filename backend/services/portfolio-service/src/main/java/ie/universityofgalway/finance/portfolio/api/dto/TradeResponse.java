package ie.universityofgalway.finance.portfolio.api.dto;

import ie.universityofgalway.finance.portfolio.domain.model.Trade;
import ie.universityofgalway.finance.portfolio.domain.model.TradeType;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeResponse(
        Long id,
        String ticker,
        TradeType type,
        BigDecimal quantity,
        BigDecimal price,
        Instant executedAt
) {
    public static TradeResponse from(Trade trade) {
        return new TradeResponse(
                trade.getId(),
                trade.getTicker(),
                trade.getType(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getExecutedAt()
        );
    }

}
