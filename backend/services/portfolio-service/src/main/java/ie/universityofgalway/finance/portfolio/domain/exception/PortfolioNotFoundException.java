package ie.universityofgalway.finance.portfolio.domain.exception;

public class PortfolioNotFoundException extends RuntimeException {

    private final Long portfolioId;

    public PortfolioNotFoundException(Long portfolioId) {
        super("Portfolio of ID: " +portfolioId +"not found");
        this.portfolioId = portfolioId;
    }

    public Long getPortfolioId() {
        return portfolioId;
    }
}