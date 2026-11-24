package ie.universityofgalway.finance.portfolio.infrastructure.persistance.entity;

import ie.universityofgalway.finance.portfolio.domain.model.TradeType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Database row in the {@code trades} table.
 *
 * <p>Linked to its {@link HoldingEntity} (column {@code holding_id}), not directly to a
 * portfolio. Quantities keep 6 decimal places and prices 4. Only used inside the infrastructure
 * layer; {@code TradeRepositoryAdapter} converts it to and from the domain
 * {@link ie.universityofgalway.finance.portfolio.domain.model.Trade}.</p>
 */
@Entity
@Table(name = "trades")
public class TradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holding_id", nullable = false)
    private HoldingEntity holding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private Instant executedAt;

    /** Required by JPA; not for application code. */
    protected TradeEntity() {
        // JPA
    }

    /**
     * Creates a new, unsaved row; the database assigns the id.
     *
     * @param holding    the saved holding this trade changed
     * @param type       buy or sell
     * @param quantity   number of shares traded
     * @param price      price per share
     * @param executedAt when the trade happened
     */
    public TradeEntity(HoldingEntity holding, TradeType type,
                       BigDecimal quantity, BigDecimal price, Instant executedAt) {
        this.holding = holding;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.executedAt = executedAt;
    }

    public Long getId() { return id; }
    public HoldingEntity getHolding() { return holding; }
    public TradeType getType() { return type; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public Instant getExecutedAt() { return executedAt; }
}