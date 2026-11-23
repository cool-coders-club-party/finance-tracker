package ie.universityofgalway.finance.portfolio.api.dto;

import ie.universityofgalway.finance.portfolio.domain.model.Portfolio;

/**
 * Response body describing a portfolio, used by list and create.
 *
 * <p>The owner's user id is deliberately left out: the caller is always the owner.</p>
 *
 * @param id   the portfolio id, used in every other portfolio URL
 * @param name the portfolio's display name
 */
public record PortfolioResponse(Long id, String name) {

    /**
     * Builds the response from a domain portfolio.
     *
     * @param portfolio the portfolio to describe
     * @return the response body
     */
    public static PortfolioResponse from(Portfolio portfolio) {
        return new PortfolioResponse(portfolio.getId(), portfolio.getName());
    }
}
