package ie.universityofgalway.finance.portfolio.infrastructure.persistence.adapter;

import ie.universityofgalway.finance.portfolio.application.port.PortfolioRepository;
import ie.universityofgalway.finance.portfolio.domain.exception.PortfolioNotFoundException;
import ie.universityofgalway.finance.portfolio.domain.model.Portfolio;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.entity.PortfolioEntity;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.jpa.JpaPortfolioRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PortfolioRepositoryAdapter implements PortfolioRepository {

    private final JpaPortfolioRepository jpa;

    public PortfolioRepositoryAdapter(JpaPortfolioRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Portfolio> findByIdAndUserId(Long id, String userId) {
        return jpa.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public List<Portfolio> findByUserId(String userId) {
        return jpa.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Portfolio save(Portfolio portfolio) {
        PortfolioEntity entity = portfolio.getId() == null
                ? new PortfolioEntity(portfolio.getUserId(), portfolio.getName())
                : jpa.findById(portfolio.getId())
                .orElseThrow(() -> new PortfolioNotFoundException(portfolio.getId()));
        entity.setName(portfolio.getName());
        return toDomain(jpa.save(entity));
    }

    private Portfolio toDomain(PortfolioEntity e) {
        return new Portfolio(e.getId(), e.getUserId(), e.getName());
    }
}
