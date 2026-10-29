package ie.universityofgalway.finance.portfolio.infrastructure.persistence.jpa;

import ie.universityofgalway.finance.portfolio.infrastructure.persistence.entity.PortfolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaPortfolioRepository extends JpaRepository<PortfolioEntity, Long> {
    Optional<PortfolioEntity> findByIdAndUserId(Long id, String userId);
    List<PortfolioEntity> findByUserId(String userId);
}