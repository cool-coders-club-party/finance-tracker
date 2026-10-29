package ie.universityofgalway.finance.portfolio.infrastructure.persistence.entity;

import jakarta.persistence.*;

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

    protected PortfolioEntity() {
        // JPA
    }

    public PortfolioEntity(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}