package ie.universityofgalway.finance.portfolio.application.port;

import ie.universityofgalway.finance.portfolio.domain.model.Trade;

import java.util.List;

public interface TradeRepository {
    Trade save(Trade trade);
    List<Trade> findAllByPortfolioId(Long portfolioId);
}