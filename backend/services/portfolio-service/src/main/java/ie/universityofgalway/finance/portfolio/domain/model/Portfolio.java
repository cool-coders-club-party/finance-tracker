    package ie.universityofgalway.finance.portfolio.domain.model;

public class Portfolio {

    private final Long id;
    private final String userId;
    private final String name;

    public Portfolio(Long id, String userId, String name) {
        this.id = id;
        this.userId = userId;
        this.name = name;
    }

    public static Portfolio create(String userId, String name) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Portfolio must have an owner");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Portfolio name must not be blank");
        }
        return new Portfolio(null, userId, name.trim());
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
}