package ie.universityofgalway.finance.portfolio.dto;

import ie.universityofgalway.finance.portfolio.model.Portfolio;

public record PortfolioResponse(Long id, String name) {

    public static PortfolioResponse from(Portfolio portfolio) {
        return new PortfolioResponse(portfolio.getId(), portfolio.getName());
    }
}
