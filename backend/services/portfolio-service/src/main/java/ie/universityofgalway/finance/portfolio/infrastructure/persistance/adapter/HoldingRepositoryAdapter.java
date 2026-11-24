package ie.universityofgalway.finance.portfolio.infrastructure.persistance.adapter;

import ie.universityofgalway.finance.portfolio.application.port.HoldingRepository;
import ie.universityofgalway.finance.portfolio.domain.model.Holding;
import ie.universityofgalway.finance.portfolio.infrastructure.persistance.entity.HoldingEntity;
import ie.universityofgalway.finance.portfolio.infrastructure.persistance.jpa.JpaHoldingRepository;
import ie.universityofgalway.finance.portfolio.infrastructure.persistance.jpa.JpaPortfolioRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Maps saved holding entities to domain positions, preserving purchase currency and cost.
 */
@Component
public class HoldingRepositoryAdapter implements HoldingRepository {

    private final JpaHoldingRepository holdings;
    private final JpaPortfolioRepository portfolios;

    /**
     * Creates the persistence adapter.
     *
     * @param holdings JPA repository for holding rows
     * @param portfolios JPA repository used to attach newly saved holdings to a portfolio
     */
    public HoldingRepositoryAdapter(JpaHoldingRepository holdings, JpaPortfolioRepository portfolios) {
        this.holdings = holdings;
        this.portfolios = portfolios;
    }

    /**
     * Finds a holding by portfolio and exact stored ticker.
     *
     * @param portfolioId owning portfolio identifier
     * @param ticker stored stock symbol
     * @return the mapped holding, or an empty optional if absent
     */
    @Override
    public Optional<Holding> findByPortfolioIdAndTicker(Long portfolioId, String ticker) {
        return holdings.findByPortfolioIdAndTicker(portfolioId, ticker).map(this::toDomain);
    }

    /**
     * Loads all saved holdings for a portfolio, including zero-share positions.
     *
     * @param portfolioId owning portfolio identifier
     * @return mapped domain holdings
     */
    @Override
    public List<Holding> findByPortfolioId(Long portfolioId) {
        return holdings.findByPortfolioId(portfolioId).stream().map(this::toDomain).toList();
    }

    /**
     * Inserts or updates quantity, average purchase price and purchase currency.
     *
     * @param holding domain holding; a null identifier selects insertion
     * @return the persisted holding, including its generated identifier
     * @throws java.util.NoSuchElementException if an existing holding identifier has no saved row
     */
    @Override
    public Holding save(Holding holding) {
        HoldingEntity entity = holding.getId() == null
                ? new HoldingEntity(portfolios.getReferenceById(holding.getPortfolioId()), holding.getTicker())
                : holdings.findById(holding.getId()).orElseThrow();
        entity.setCurrency(holding.getCurrency());
        entity.setQuantity(holding.getQuantity());
        entity.setAvgPurchasePrice(holding.getAvgPurchasePrice());
        return toDomain(holdings.save(entity));
    }

    /**
     * Copies saved values without inferring a missing purchase currency from a quote.
     *
     * @param e saved holding entity
     * @return the reconstructed domain holding
     */
    private Holding toDomain(HoldingEntity e) {
        return new Holding(e.getId(), e.getPortfolio().getId(), e.getTicker(),
                e.getQuantity(), e.getAvgPurchasePrice(), e.getCurrency());
    }
}