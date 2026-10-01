package ie.universityofgalway.finance.portfolio.repository;

import ie.universityofgalway.finance.portfolio.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    // Scoped by userId so a user can never load a portfolio that isn't theirs,
    // even if they guess a valid id.
    Optional<Portfolio> findByIdAndUserId(Long id, String userId);
}
