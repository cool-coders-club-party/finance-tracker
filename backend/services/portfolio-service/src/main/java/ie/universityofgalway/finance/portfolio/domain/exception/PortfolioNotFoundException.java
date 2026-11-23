package ie.universityofgalway.finance.portfolio.domain.exception;

/**
 * Thrown when a portfolio doesn't exist or belongs to another user.
 *
 * <p>Both cases deliberately use the same exception, so the API (which returns
 * {@code 404 NOT_FOUND}) never reveals whether another user's portfolio id exists.</p>
 */
public class PortfolioNotFoundException extends RuntimeException {

    private final Long portfolioId;

    /**
     * @param portfolioId the id that was requested
     */
    public PortfolioNotFoundException(Long portfolioId) {
        super("Portfolio of ID: " +portfolioId +" not found");
        this.portfolioId = portfolioId;
    }

    /**
     * @return the id that was requested
     */
    public Long getPortfolioId() {
        return portfolioId;
    }
}