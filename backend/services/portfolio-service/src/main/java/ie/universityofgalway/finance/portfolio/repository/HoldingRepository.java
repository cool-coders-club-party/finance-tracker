package ie.universityofgalway.finance.portfolio.repository;

import ie.universityofgalway.finance.portfolio.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    Optional<Holding> findByPortfolioIdAndTicker(Long portfolioId, String ticker);
}
