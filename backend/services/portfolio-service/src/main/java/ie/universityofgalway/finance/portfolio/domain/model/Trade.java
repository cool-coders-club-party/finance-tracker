package ie.universityofgalway.finance.portfolio.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public class Trade {

    private final Long id;
    private final Long portfolioId;
    private final String ticker;
    private final TradeType type;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private final Instant executedAt;

    public Trade(Long id, Long portfolioId, String ticker, TradeType type,
                 BigDecimal quantity, BigDecimal price, Instant executedAt) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.ticker = ticker;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.executedAt = executedAt;
    }

    public Long getId() { return id; }
    public Long getPortfolioId() { return portfolioId; }
    public String getTicker() { return ticker; }
    public TradeType getType() { return type; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public Instant getExecutedAt() { return executedAt; }
}