package ie.universityofgalway.finance.portfolio.domain.model;

import ie.universityofgalway.finance.portfolio.domain.exception.InsufficientSharesException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * A portfolio position with a running weighted average purchase price.
 *
 * <p>Purchase currency is saved independently of market quotes and may be unknown for
 * legacy holdings. Domain mutations return trades; callers persist the holding and trade.</p>
 */
public class Holding {

    private final Long id;
    private final Long portfolioId;
    private final String ticker;
    private final String currency;
    private BigDecimal quantity;
    private BigDecimal avgPurchasePrice;

    /**
     * Reconstructs a holding whose purchase currency is unknown.
     *
     * @param id saved holding identifier, or null before persistence
     * @param portfolioId owning portfolio identifier
     * @param ticker stock symbol
     * @param quantity current share quantity
     * @param avgPurchasePrice weighted average purchase price per share
     */
    public Holding(Long id, Long portfolioId, String ticker,
                   BigDecimal quantity, BigDecimal avgPurchasePrice) {
        this(id, portfolioId, ticker, quantity, avgPurchasePrice, null);
    }

    /**
     * Reconstructs a holding from saved values without recalculating its cost.
     *
     * @param id saved holding identifier, or null before persistence
     * @param portfolioId owning portfolio identifier
     * @param ticker stock symbol
     * @param quantity current share quantity
     * @param avgPurchasePrice weighted average purchase price per share
     * @param currency saved purchase currency, or null when unknown
     */
    public Holding(Long id, Long portfolioId, String ticker,
                   BigDecimal quantity, BigDecimal avgPurchasePrice, String currency) {
        this.currency = currency;
        this.id = id;
        this.portfolioId = portfolioId;
        this.ticker = ticker;
        this.quantity = quantity;
        this.avgPurchasePrice = avgPurchasePrice;
    }

    /**
     * Opens an unsaved zero-share holding with unknown purchase currency.
     *
     * @param portfolioId owning portfolio identifier
     * @param ticker stock symbol
     * @return a holding with zero quantity and average price
     */
    public static Holding open(Long portfolioId, String ticker) {
        return open(portfolioId, ticker, null);
    }

    /**
     * Opens an unsaved zero-share holding in the supplied purchase currency.
     *
     * @param portfolioId owning portfolio identifier
     * @param ticker stock symbol
     * @param currency purchase currency, or null when unknown
     * @return a holding with zero quantity and average price
     */
    public static Holding open(Long portfolioId, String ticker, String currency) {
        return new Holding(null, portfolioId, ticker, BigDecimal.ZERO, BigDecimal.ZERO, currency);
    }

    /**
     * Adds shares and recalculates the weighted average cost to four decimal places using HALF_UP.
     *
     * <p>This mutates the holding; it does not persist either the holding or the returned trade.</p>
     *
     * @param qty strictly positive number of shares
     * @param price execution price per share in the holding currency, supplied by the caller
     * @param at execution timestamp supplied by the caller
     * @return an unsaved BUY trade
     * @throws IllegalArgumentException if the quantity is null or not positive
     */
    public Trade buy(BigDecimal qty, BigDecimal price, Instant at) {
        requirePositive(qty);
        BigDecimal newQuantity = quantity.add(qty);
        avgPurchasePrice = quantity.multiply(avgPurchasePrice)
                .add(qty.multiply(price))
                .divide(newQuantity, 4, RoundingMode.HALF_UP);
        quantity = newQuantity;
        return new Trade(null, portfolioId, ticker, TradeType.BUY, qty, price, at);
    }

    /**
     * Removes shares without changing the weighted average purchase price.
     *
     * <p>This mutates the holding; fully sold positions retain their previous average cost.</p>
     *
     * @param qty strictly positive number of shares
     * @param price execution price per share in the holding currency, supplied by the caller
     * @param at execution timestamp supplied by the caller
     * @return an unsaved SELL trade
     * @throws IllegalArgumentException if the quantity is null or not positive
     * @throws InsufficientSharesException if the requested quantity exceeds the shares held
     */
    public Trade sell(BigDecimal qty, BigDecimal price, Instant at) {
        requirePositive(qty);
        if (quantity.compareTo(qty) < 0) {
            throw new InsufficientSharesException(ticker, qty, quantity);
        }
        quantity = quantity.subtract(qty);
        return new Trade(null, portfolioId, ticker, TradeType.SELL, qty, price, at);
    }

    /**
     * Rejects missing, zero and negative trade quantities.
     *
     * @param qty quantity to validate
     * @throws IllegalArgumentException if the quantity is null or not positive
     */
    private static void requirePositive(BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    /**
     * Returns the saved purchase currency, or null when unknown.
     *
     * @return the saved purchase currency, or null when unknown
     */
    public String getCurrency() { return currency; }

    /**
     * Returns the saved identifier, or null before persistence.
     *
     * @return the saved identifier, or null before persistence
     */
    public Long getId() { return id; }
    /**
     * Returns the owning portfolio identifier.
     *
     * @return the owning portfolio identifier
     */
    public Long getPortfolioId() { return portfolioId; }
    /**
     * Returns the stock symbol.
     *
     * @return the stock symbol
     */
    public String getTicker() { return ticker; }
    /**
     * Returns the current share quantity.
     *
     * @return the current share quantity
     */
    public BigDecimal getQuantity() { return quantity; }
    /**
     * Returns the weighted average purchase price per share.
     *
     * @return the weighted average purchase price per share
     */
    public BigDecimal getAvgPurchasePrice() { return avgPurchasePrice; }
}