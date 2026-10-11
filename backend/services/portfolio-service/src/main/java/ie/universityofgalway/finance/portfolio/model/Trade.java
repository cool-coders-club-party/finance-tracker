package ie.universityofgalway.finance.portfolio.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holding_id", nullable = false)
    private Holding holding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private Instant executedAt;

    protected Trade() {
        // JPA
    }

    public Trade(Holding holding, TradeType type, BigDecimal quantity, BigDecimal price, Instant executedAt) {
        this.holding = holding;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.executedAt = executedAt;
    }

    public Long getId() { return id; }
    public Holding getHolding() { return holding; }
    public TradeType getType() { return type; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public Instant getExecutedAt() { return executedAt; }
}
