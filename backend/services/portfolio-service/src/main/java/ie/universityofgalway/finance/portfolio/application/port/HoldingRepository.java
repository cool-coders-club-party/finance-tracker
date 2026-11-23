package ie.universityofgalway.finance.portfolio.application.port;

import ie.universityofgalway.finance.portfolio.domain.model.Holding;


import java.util.List;
import java.util.Optional;

/**
 * Port for storing and retrieving {@link Holding}s.
 *
 * <p>Implemented in the infrastructure layer by {@code HoldingRepositoryAdapter}.
 * A portfolio has at most one holding per ticker.</p>
 */
public interface HoldingRepository {

    /**
     * Finds the holding for one stock in a portfolio.
     *
     * @param portfolioId the portfolio id
     * @param ticker      the stock symbol, upper case
     * @return the holding, or empty if the portfolio has never held that stock
     */
    Optional<Holding> findByPortfolioIdAndTicker(Long portfolioId, String ticker);

    /**
     * Lists every holding in a portfolio, including ones sold down to zero shares.
     *
     * @param portfolioId the portfolio id
     * @return the holdings, or an empty list
     */
    List<Holding> findByPortfolioId(Long portfolioId);

    /**
     * Inserts a new holding or updates an existing one.
     *
     * <p>Must be called before saving the trade that changed it, because a trade is stored
     * against its holding.</p>
     *
     * @param holding the holding to store; a {@code null} id means it is new
     * @return the stored holding, with its id
     */
    Holding save(Holding holding);
}
