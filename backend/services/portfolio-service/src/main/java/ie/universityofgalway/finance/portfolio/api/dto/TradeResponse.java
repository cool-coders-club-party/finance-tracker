package ie.universityofgalway.finance.portfolio.api.dto;

import ie.universityofgalway.finance.portfolio.domain.model.Trade;
import ie.universityofgalway.finance.portfolio.domain.model.TradeType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response item for {@code GET /api/portfolios/{id}/trades}: one buy or sell.
 *
 * @param id         the trade id
 * @param ticker     the stock symbol
 * @param type       {@code BUY} or {@code SELL}
 * @param quantity   number of shares traded
 * @param price      price per share: the market price at the time, or the price actually paid
 *                   for a recorded earlier purchase
 * @param executedAt when the trade happened
 */
public record TradeResponse(
        Long id,
        String ticker,
        TradeType type,
        BigDecimal quantity,
        BigDecimal price,
        Instant executedAt
) {
    /**
     * Builds the response from a domain trade.
     *
     * @param trade the trade to describe
     * @return the response item
     */
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
