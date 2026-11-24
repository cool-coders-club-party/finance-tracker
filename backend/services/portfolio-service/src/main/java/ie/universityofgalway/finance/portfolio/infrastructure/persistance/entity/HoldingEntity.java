package ie.universityofgalway.finance.portfolio.infrastructure.persistance.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * JPA row for a portfolio holding, unique by portfolio and ticker.
 *
 * <p>Quantity uses six decimal places and average purchase price uses four. Currency
 * is nullable for legacy rows and must not be inferred from a later market quote.</p>
 */
@Entity
@Table(name = "holdings", uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "ticker"}))
public class HoldingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;

    @Column(nullable = false, length = 5)
    private String ticker;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal avgPurchasePrice = BigDecimal.ZERO;


    // Nullable for legacy rows: their purchase currency cannot be inferred from a live quote.
    @Column(length = 3)
    private String currency;

    /**
     * Returns the saved currency, or null for an unknown purchase currency.
     *
     * @return the saved currency, or null for an unknown purchase currency
     */
    public String getCurrency() { return currency; }
    /**
     * Sets the saved purchase currency.
     *
     * @param currency purchase currency, or null when unknown
     */
    public void setCurrency(String currency) { this.currency = currency; }

    /**
     * Creates an empty entity for JPA hydration.
     */
    protected HoldingEntity() {
        // JPA
    }

    /**
     * Creates an unsaved zero-share position; the adapter supplies cost and currency.
     *
     * @param portfolio owning portfolio entity
     * @param ticker stock symbol
     */
    public HoldingEntity(PortfolioEntity portfolio, String ticker) {
        this.portfolio = portfolio;
        this.ticker = ticker;
    }

    /**
     * Returns the generated identifier, or null before insertion.
     *
     * @return the generated identifier, or null before insertion
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the owning portfolio, potentially a lazy JPA reference.
     *
     * @return the owning portfolio, potentially a lazy JPA reference
     */
    public PortfolioEntity getPortfolio() {
        return portfolio;
    }

    /**
     * Returns the stored stock symbol.
     *
     * @return the stored stock symbol
     */
    public String getTicker() {
        return ticker;
    }

    /**
     * Returns the saved share quantity.
     *
     * @return the saved share quantity
     */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /**
     * Sets the saved holding value supplied by the domain.
     *
     * @param quantity share quantity to persist
     */
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    /**
     * Returns the saved weighted average purchase price per share.
     *
     * @return the saved weighted average purchase price per share
     */
    public BigDecimal getAvgPurchasePrice() {
        return avgPurchasePrice;
    }

    /**
     * Sets the saved holding value supplied by the domain.
     *
     * @param avgPurchasePrice weighted average purchase price per share to persist
     */
    public void setAvgPurchasePrice(BigDecimal avgPurchasePrice) {
        this.avgPurchasePrice = avgPurchasePrice;
    }
}