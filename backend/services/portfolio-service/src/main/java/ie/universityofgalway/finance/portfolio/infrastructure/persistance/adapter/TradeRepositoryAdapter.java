package ie.universityofgalway.finance.portfolio.infrastructure.persistance.adapter;

import ie.universityofgalway.finance.portfolio.application.port.TradeRepository;
import ie.universityofgalway.finance.portfolio.domain.model.Trade;
import ie.universityofgalway.finance.portfolio.infrastructure.persistance.entity.HoldingEntity;
import ie.universityofgalway.finance.portfolio.infrastructure.persistance.entity.TradeEntity;
import ie.universityofgalway.finance.portfolio.infrastructure.persistance.jpa.JpaHoldingRepository;
import ie.universityofgalway.finance.portfolio.infrastructure.persistance.jpa.JpaTradeRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JPA implementation of the {@link TradeRepository} port.
 *
 * <p>In the database a trade belongs to a holding rather than directly to a portfolio, so
 * saving looks up the holding by portfolio and ticker, and loading reads the portfolio id and
 * ticker back through it. Translates between the domain {@link Trade} and {@link TradeEntity}.</p>
 */
@Component
public class TradeRepositoryAdapter implements TradeRepository {

    private final JpaTradeRepository trades;
    private final JpaHoldingRepository holdings;

    public TradeRepositoryAdapter(JpaTradeRepository trades, JpaHoldingRepository holdings) {
        this.trades = trades;
        this.holdings = holdings;
    }

    @Override
    public Trade save(Trade trade) {
        HoldingEntity holding = holdings
                .findByPortfolioIdAndTicker(trade.getPortfolioId(), trade.getTicker())
                .orElseThrow(() -> new IllegalStateException(
                        "Holding must be saved before its trades are recorded"));

        TradeEntity saved = trades.save(new TradeEntity(
                holding, trade.getType(), trade.getQuantity(),
                trade.getPrice(), trade.getExecutedAt()));

        return toDomain(saved);
    }


    @Override
    public List<Trade> findAllByPortfolioId(Long portfolioId) {
        return trades.findAllByPortfolioId(portfolioId).stream()
                .map(this::toDomain)
                .toList();
    }

    private Trade toDomain(TradeEntity e) {
        return new Trade(
                e.getId(),
                e.getHolding().getPortfolio().getId(),
                e.getHolding().getTicker(),
                e.getType(),
                e.getQuantity(),
                e.getPrice(),
                e.getExecutedAt());
    }
}