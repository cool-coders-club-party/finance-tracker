package ie.universityofgalway.finance.portfolio.infrastructure.persistance.jpa;

import ie.universityofgalway.finance.portfolio.infrastructure.persistance.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data repository for {@link TradeEntity}. Used only by {@code TradeRepositoryAdapter}.
 */
public interface JpaTradeRepository extends JpaRepository<TradeEntity, Long> {

    /**
     * Lists every trade in a portfolio, across all its holdings.
     *
     * <p>Fetches each trade's holding in the same query, because the adapter reads the ticker
     * and portfolio id from it. Ordered oldest first, with ties broken by id, so performance
     * replays trades in the order they happened.</p>
     *
     * @param portfolioId the portfolio id
     * @return the trades, oldest first
     */

    @Query("""
            select t from TradeEntity t
            join fetch t.holding h
            where h.portfolio.id = :portfolioId
            order by t.executedAt asc, t.id asc
            """)
    List<TradeEntity> findAllByPortfolioId(@Param("portfolioId") Long portfolioId);

}