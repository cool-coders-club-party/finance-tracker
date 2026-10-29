package ie.universityofgalway.finance.portfolio.infrastructure.persistence.jpa;

import ie.universityofgalway.finance.portfolio.infrastructure.persistence.entity.HoldingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaHoldingRepository extends JpaRepository<HoldingEntity, Long> {
    Optional<HoldingEntity> findByPortfolioIdAndTicker(Long portfolioId, String ticker);
    List<HoldingEntity> findByPortfolioId(Long portfolioId);
}
