package ie.universityofgalway.finance.portfolio.infrastructure.persistance.jpa;

import ie.universityofgalway.finance.portfolio.infrastructure.persistance.entity.HoldingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link HoldingEntity}. Spring generates the queries from the
 * method names. Used only by the persistence adapters.
 */
public interface JpaHoldingRepository extends JpaRepository<HoldingEntity, Long> {

    /**
     * @param portfolioId the portfolio id
     * @param ticker      the stock symbol, upper case
     * @return the holding, or empty if there is none
     */
    Optional<HoldingEntity> findByPortfolioIdAndTicker(Long portfolioId, String ticker);

    /**
     * @param portfolioId the portfolio id
     * @return every holding in the portfolio
     */
    List<HoldingEntity> findByPortfolioId(Long portfolioId);
}
