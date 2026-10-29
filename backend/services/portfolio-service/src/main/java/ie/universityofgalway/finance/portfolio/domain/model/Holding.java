package ie.universityofgalway.finance.portfolio.domain.model;

import ie.universityofgalway.finance.portfolio.domain.exception.InsufficientSharesException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public class Holding {

    private final Long id;
    private final Long portfolioId;
    private final String ticker;
    private BigDecimal quantity;
    private BigDecimal avgPurchasePrice;

    public Holding(Long id, Long portfolioId, String ticker,
                   BigDecimal quantity, BigDecimal avgPurchasePrice) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.ticker = ticker;
        this.quantity = quantity;
        this.avgPurchasePrice = avgPurchasePrice;
    }

    public static Holding open(Long portfolioId, String ticker) {
        return new Holding(null, portfolioId, ticker, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public Trade buy(BigDecimal qty, BigDecimal price, Instant at) {
        requirePositive(qty);
        BigDecimal newQuantity = quantity.add(qty);
        avgPurchasePrice = quantity.multiply(avgPurchasePrice)
                .add(qty.multiply(price))
                .divide(newQuantity, 4, RoundingMode.HALF_UP);
        quantity = newQuantity;
        return new Trade(null, portfolioId, ticker, TradeType.BUY, qty, price, at);
    }

    public Trade sell(BigDecimal qty, BigDecimal price, Instant at) {
        requirePositive(qty);
        if (quantity.compareTo(qty) < 0) {
            throw new InsufficientSharesException(ticker, qty, quantity);
        }
        quantity = quantity.subtract(qty);
        return new Trade(null, portfolioId, ticker, TradeType.SELL, qty, price, at);
    }

    private static void requirePositive(BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    public Long getId() { return id; }
    public Long getPortfolioId() { return portfolioId; }
    public String getTicker() { return ticker; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getAvgPurchasePrice() { return avgPurchasePrice; }
}