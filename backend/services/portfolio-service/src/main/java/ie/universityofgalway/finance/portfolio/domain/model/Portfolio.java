    package ie.universityofgalway.finance.portfolio.domain.model;

/**
 * A named collection of holdings owned by one user.
 *
 * <p>Domain model with no Spring or database code. Created with {@link #create(String, String)},
 * which enforces its rules, and rebuilt from storage with the constructor.</p>
 */
public class Portfolio {

    private final Long id;
    private final String userId;
    private final String name;

    /**
     * Rebuilds a portfolio exactly as given, without validation. Used when loading from storage.
     *
     * @param id     the portfolio id, or {@code null} if not yet saved
     * @param userId the owner's user id
     * @param name   the display name
     */
    public Portfolio(Long id, String userId, String name) {
        this.id = id;
        this.userId = userId;
        this.name = name;
    }

    /**
     * Creates a new, unsaved portfolio.
     *
     * @param userId the owner's user id; must not be blank
     * @param name   the display name; must not be blank, and surrounding spaces are removed
     * @return a portfolio with a {@code null} id
     * @throws IllegalArgumentException if either value is blank
     */
    public static Portfolio create(String userId, String name) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Portfolio must have an owner");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Portfolio name must not be blank");
        }
        return new Portfolio(null, userId, name.trim());
    }

    /** @return the portfolio id, or {@code null} if not yet saved */
    public Long getId() { return id; }
    /** @return the owner's user id (the subject of their access token) */
    public String getUserId() { return userId; }
    /** @return the display name */
    public String getName() { return name; }
}