package ie.universityofgalway.finance.portfolio.application.port;

import ie.universityofgalway.finance.portfolio.domain.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository {
    Optional<Holding> findByPortfolioIdAndTicker(Long portfolioId, String ticker);
    List<Holding> findByPortfolioId(Long portfolioId);
    Holding save(Holding holding);
}
