package ie.universityofgalway.finance.portfolio.application.port;

import ie.universityofgalway.finance.portfolio.domain.model.Portfolio;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository {
    Optional<Portfolio> findByIdAndUserId(Long id, String userId);
    List<Portfolio> findByUserId(String userId);
    Portfolio save(Portfolio portfolio);
}
