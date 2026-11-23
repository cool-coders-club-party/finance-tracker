package ie.universityofgalway.finance.portfolio.application.port;

import ie.universityofgalway.finance.portfolio.domain.model.Portfolio;

import java.util.List;
import java.util.Optional;

/**
 * Port for storing and retrieving {@link Portfolio}s.
 *
 * <p>Implemented in the infrastructure layer by {@code PortfolioRepositoryAdapter}.</p>
 */
public interface PortfolioRepository {

    /**
     * Finds a portfolio only if it belongs to the given user. This is the ownership check used
     * before every portfolio operation.
     *
     * @param id     the portfolio id
     * @param userId the user id from the caller's access token
     * @return the portfolio, or empty if it doesn't exist or belongs to someone else
     */
    Optional<Portfolio> findByIdAndUserId(Long id, String userId);

    /**
     * Lists a user's portfolios.
     *
     * @param userId the owner's user id
     * @return the portfolios, or an empty list
     */
    List<Portfolio> findByUserId(String userId);

    /**
     * Inserts a new portfolio or updates an existing one's name.
     *
     * @param portfolio the portfolio to store; a {@code null} id means it is new
     * @return the stored portfolio, with its id
     */
    Portfolio save(Portfolio portfolio);
}
