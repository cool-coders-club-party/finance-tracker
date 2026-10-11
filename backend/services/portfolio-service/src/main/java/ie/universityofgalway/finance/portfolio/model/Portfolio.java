package ie.universityofgalway.finance.portfolio.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "portfolios")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Holding> holdings = new ArrayList<>();

    protected Portfolio() {
        // JPA
    }

    public Portfolio(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public List<Holding> getHoldings() { return holdings; }
    public void addHolding(Holding holding) { holdings.add(holding); }
}
