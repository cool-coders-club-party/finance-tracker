package ie.universityofgalway.finance.portfolio.application.port;

import ie.universityofgalway.finance.portfolio.domain.model.Trade;

import java.util.List;

/**
 * Port for storing and retrieving {@link Trade}s.
 *
 * <p>Implemented in the infrastructure layer by {@code TradeRepositoryAdapter}.
 * Trades are never updated or deleted once saved.</p>
 */
public interface TradeRepository {

    /**
     * Stores a trade against its holding.
     *
     * @param trade the trade to store
     * @return the stored trade, with its id
     * @throws IllegalStateException if the trade's holding hasn't been saved yet
     */
    Trade save(Trade trade);

    /**
     * Lists every trade in a portfolio, across all stocks.
     *
     * @param portfolioId the portfolio id
     * @return the trades ordered by {@code executedAt}, oldest first (ties broken by id)
     */
    List<Trade> findAllByPortfolioId(Long portfolioId);
}