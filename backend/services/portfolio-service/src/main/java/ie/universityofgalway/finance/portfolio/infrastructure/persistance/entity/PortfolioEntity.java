package ie.universityofgalway.finance.portfolio.infrastructure.persistance.entity;

import jakarta.persistence.*;

/**
 * Database row in the {@code portfolios} table.
 *
 * <p>Only used inside the infrastructure layer; {@code PortfolioRepositoryAdapter} converts it
 * to and from the domain {@link ie.universityofgalway.finance.portfolio.domain.model.Portfolio}.
 * The table is created and updated by Hibernate ({@code ddl-auto: update}).</p>
 */
@Entity
@Table(name = "portfolios")
public class PortfolioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    /** Required by JPA; not for application code. */
    protected PortfolioEntity() {
        // JPA
    }

    /**
     * Creates a new, unsaved row; the database assigns the id.
     *
     * @param userId the owner's user id
     * @param name   the display name
     */
    public PortfolioEntity(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}