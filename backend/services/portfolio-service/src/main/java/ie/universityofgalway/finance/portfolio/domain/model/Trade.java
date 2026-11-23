package ie.universityofgalway.finance.portfolio.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One buy or sell of a stock in a portfolio.
 *
 * <p>Created by {@link Holding#buy} and {@link Holding#sell}, so a trade always matches the
 * change it made to its holding. Immutable, and never edited or deleted once saved: the trade
 * history is what performance is calculated from.</p>
 */
public class Trade {

    private final Long id;
    private final Long portfolioId;
    private final String ticker;
    private final TradeType type;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private final Instant executedAt;

    /**
     * @param id          the trade id, or {@code null} if not yet saved
     * @param portfolioId the portfolio the trade belongs to
     * @param ticker      the stock symbol, upper case
     * @param type        {@link TradeType#BUY} or {@link TradeType#SELL}
     * @param quantity    number of shares traded, greater than zero
     * @param price       price per share
     * @param executedAt  when the trade happened
     */
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