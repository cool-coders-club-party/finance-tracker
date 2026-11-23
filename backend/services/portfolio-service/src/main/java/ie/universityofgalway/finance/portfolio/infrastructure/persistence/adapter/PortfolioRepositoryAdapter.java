package ie.universityofgalway.finance.portfolio.infrastructure.persistence.adapter;

import ie.universityofgalway.finance.portfolio.application.port.PortfolioRepository;
import ie.universityofgalway.finance.portfolio.domain.exception.PortfolioNotFoundException;
import ie.universityofgalway.finance.portfolio.domain.model.Portfolio;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.entity.PortfolioEntity;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.jpa.JpaPortfolioRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of the {@link PortfolioRepository} port.
 *
 * <p>Stores portfolios through a Spring Data repository and translates between the domain
 * {@link Portfolio} and the database-facing {@link PortfolioEntity}, so the rest of the
 * application never sees JPA types.</p>
 */
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

    /**
     * {@inheritDoc}
     *
     * <p>For an existing portfolio only the name is updated; the owner never changes.</p>
     *
     * @throws PortfolioNotFoundException if the portfolio has an id but no longer exists
     */
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
