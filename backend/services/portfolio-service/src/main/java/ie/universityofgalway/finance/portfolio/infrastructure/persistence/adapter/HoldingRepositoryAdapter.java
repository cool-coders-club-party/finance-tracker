package ie.universityofgalway.finance.portfolio.infrastructure.persistence.adapter;

import ie.universityofgalway.finance.portfolio.application.port.HoldingRepository;
import ie.universityofgalway.finance.portfolio.domain.model.Holding;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.entity.HoldingEntity;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.jpa.JpaHoldingRepository;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.jpa.JpaPortfolioRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class HoldingRepositoryAdapter implements HoldingRepository {

    private final JpaHoldingRepository holdings;
    private final JpaPortfolioRepository portfolios;

    public HoldingRepositoryAdapter(JpaHoldingRepository holdings, JpaPortfolioRepository portfolios) {
        this.holdings = holdings;
        this.portfolios = portfolios;
    }

    @Override
    public Optional<Holding> findByPortfolioIdAndTicker(Long portfolioId, String ticker) {
        return holdings.findByPortfolioIdAndTicker(portfolioId, ticker).map(this::toDomain);
    }

    @Override
    public List<Holding> findByPortfolioId(Long portfolioId) {
        return holdings.findByPortfolioId(portfolioId).stream().map(this::toDomain).toList();
    }

    @Override
    public Holding save(Holding holding) {
        HoldingEntity entity = holding.getId() == null
                ? new HoldingEntity(portfolios.getReferenceById(holding.getPortfolioId()), holding.getTicker())
                : holdings.findById(holding.getId()).orElseThrow();
        entity.setQuantity(holding.getQuantity());
        entity.setAvgPurchasePrice(holding.getAvgPurchasePrice());
        return toDomain(holdings.save(entity));
    }

    private Holding toDomain(HoldingEntity e) {
        return new Holding(e.getId(), e.getPortfolio().getId(), e.getTicker(),
                e.getQuantity(), e.getAvgPurchasePrice());
    }
}