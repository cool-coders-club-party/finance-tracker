package ie.universityofgalway.finance.portfolio.infrastructure.persistance.jpa;

import ie.universityofgalway.finance.portfolio.infrastructure.persistance.entity.PortfolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link PortfolioEntity}. Spring generates the queries from the
 * method names. Used only by {@code PortfolioRepositoryAdapter}.
 */
public interface JpaPortfolioRepository extends JpaRepository<PortfolioEntity, Long> {

    /**
     * @param id     the portfolio id
     * @param userId the owner's user id
     * @return the portfolio, or empty if it doesn't exist or belongs to someone else
     */
    Optional<PortfolioEntity> findByIdAndUserId(Long id, String userId);

    /**
     * @param userId the owner's user id
     * @return the user's portfolios
     */
    List<PortfolioEntity> findByUserId(String userId);
}