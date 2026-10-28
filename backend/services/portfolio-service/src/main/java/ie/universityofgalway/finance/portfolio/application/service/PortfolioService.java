package ie.universityofgalway.finance.portfolio.application.service;

import ie.universityofgalway.finance.portfolio.application.port.HoldingRepository;
import ie.universityofgalway.finance.portfolio.application.port.PortfolioRepository;
import ie.universityofgalway.finance.portfolio.application.port.PriceClient;
import ie.universityofgalway.finance.portfolio.application.port.TradeRepository;
import ie.universityofgalway.finance.portfolio.domain.exception.InsufficientSharesException;
import ie.universityofgalway.finance.portfolio.domain.exception.PortfolioNotFoundException;
import ie.universityofgalway.finance.portfolio.domain.model.Holding;
import ie.universityofgalway.finance.portfolio.domain.model.Portfolio;
import ie.universityofgalway.finance.portfolio.domain.model.Trade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final TradeRepository tradeRepository;
    private final PriceClient priceClient;

    public PortfolioService(PortfolioRepository portfolioRepository,
                            HoldingRepository holdingRepository,
                            TradeRepository tradeRepository,
                            PriceClient priceClient) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.tradeRepository = tradeRepository;
        this.priceClient = priceClient;
    }

    @Transactional
    public Holding buy(Long portfolioId, String userId, String ticker, BigDecimal quantity) {
        String symbol = ticker.toUpperCase();
        requireOwnedPortfolio(portfolioId, userId);

        BigDecimal price = priceClient.getPrice(symbol);

        Holding holding = holdingRepository.findByPortfolioIdAndTicker(portfolioId, symbol)
                .orElseGet(() -> Holding.open(portfolioId, symbol));

        Trade trade = holding.buy(quantity, price, Instant.now());

        Holding saved = holdingRepository.save(holding);   // holding first...
        tradeRepository.save(trade);                       // ...then its trade
        return saved;
    }

    @Transactional
    public Holding sell(Long portfolioId, String userId, String ticker, BigDecimal quantity) {
        String symbol = ticker.toUpperCase();
        requireOwnedPortfolio(portfolioId, userId);

        Holding holding = holdingRepository.findByPortfolioIdAndTicker(portfolioId, symbol)
                .orElseThrow(() -> new InsufficientSharesException(symbol, quantity, BigDecimal.ZERO));

        BigDecimal price = priceClient.getPrice(symbol);

        Trade trade = holding.sell(quantity, price, Instant.now());

        Holding saved = holdingRepository.save(holding);
        tradeRepository.save(trade);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Portfolio> getPortfolios(String userId) {
        return portfolioRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Holding> getHoldings(Long portfolioId, String userId) {
        requireOwnedPortfolio(portfolioId, userId);
        return holdingRepository.findByPortfolioId(portfolioId);
    }

    @Transactional(readOnly = true)
    public List<Trade> getTrades(Long portfolioId, String userId) {
        requireOwnedPortfolio(portfolioId, userId);
        return tradeRepository.findAllByPortfolioId(portfolioId);
    }

    @Transactional
    public Portfolio createPortfolio(String userId, String name) {
        return portfolioRepository.save(Portfolio.create(userId, name));
    }

    private void requireOwnedPortfolio(Long portfolioId, String userId) {
        portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
    }
}