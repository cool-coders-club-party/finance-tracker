package ie.universityofgalway.finance.portfolio.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
//A holding contains info on the amount of a particular stock, and how much its worth
@Entity
@Table(name = "holdings", uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "ticker"}))
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false, length = 5)
    private String ticker;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal avgPurchasePrice = BigDecimal.ZERO;

    @OneToMany(mappedBy = "holding", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trade> trades = new ArrayList<>();

    protected Holding() {
        // JPA
    }

    public Holding(Portfolio portfolio, String ticker) {
        this.portfolio = portfolio;
        this.ticker = ticker;
    }

    public Long getId() { return id; }
    public Portfolio getPortfolio() { return portfolio; }
    public String getTicker() { return ticker; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getAvgPurchasePrice() { return avgPurchasePrice; }
    public void setAvgPurchasePrice(BigDecimal avgPurchasePrice) { this.avgPurchasePrice = avgPurchasePrice; }
    public List<Trade> getTrades() { return trades; }
    public void addTrade(Trade trade) { trades.add(trade); }
}
