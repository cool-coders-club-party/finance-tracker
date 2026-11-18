package ie.universityofgalway.finance.portfolio.application.service;

import ie.universityofgalway.finance.portfolio.application.port.HoldingRepository;
import ie.universityofgalway.finance.portfolio.application.port.PortfolioRepository;
import ie.universityofgalway.finance.portfolio.application.port.PriceClient;
import ie.universityofgalway.finance.portfolio.application.port.TradeRepository;
import ie.universityofgalway.finance.portfolio.domain.exception.InsufficientSharesException;
import ie.universityofgalway.finance.portfolio.domain.exception.PortfolioNotFoundException;
import ie.universityofgalway.finance.portfolio.domain.model.Holding;
import ie.universityofgalway.finance.portfolio.domain.model.MarketQuote;
import ie.universityofgalway.finance.portfolio.domain.model.Portfolio;
import ie.universityofgalway.finance.portfolio.domain.model.Trade;
import ie.universityofgalway.finance.portfolio.domain.model.TradeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
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

    // Clocks differ slightly between browser and server; don't reject "just now".
    private static final Duration CLOCK_TOLERANCE = Duration.ofMinutes(1);

    /** Buys at the live market price, now. */
    @Transactional
    public Holding buy(Long portfolioId, String userId, String ticker, BigDecimal quantity) {
        return buy(portfolioId, userId, ticker, quantity, null, null);
    }

    /**
     * Buys shares, or records a purchase made earlier when {@code pricePaid} and
     * {@code executedAt} are both given (e.g. bought yesterday, entered today).
     */
    @Transactional
    public Holding buy(Long portfolioId, String userId, String ticker, BigDecimal quantity,
                       BigDecimal pricePaid, Instant executedAt) {
        if ((pricePaid == null) != (executedAt == null)) {
            throw new IllegalArgumentException(
                    "To record an earlier purchase, give both the price paid and the date");
        }
        if (pricePaid != null && pricePaid.signum() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }

        String symbol = ticker.toUpperCase();
        requireOwnedPortfolio(portfolioId, userId);

        Instant now = Instant.now();
        if (executedAt != null && executedAt.isAfter(now.plus(CLOCK_TOLERANCE))) {
            throw new IllegalArgumentException("Purchase date cannot be in the future");
        }

        // The quote is still needed for a past purchase: it confirms the ticker exists
        // and gives the currency, without which valuation reports CURRENCY_MISSING.
        MarketQuote quote = priceClient.getQuote(symbol);
        BigDecimal price = pricePaid != null ? pricePaid : quote.price();
        Instant tradeTime = executedAt != null ? executedAt : now;

        if (executedAt != null) {
            requireNoLaterSale(portfolioId, symbol, executedAt);
        }

        Holding holding = holdingRepository.findByPortfolioIdAndTicker(portfolioId, symbol)
                .orElseGet(() -> Holding.open(portfolioId, symbol, quote.currency()));

        Trade trade = holding.buy(quantity, price, tradeTime);

        Holding saved = holdingRepository.save(holding);   // holding first...
        tradeRepository.save(trade);                       // ...then its trade
        return saved;
    }

    // The holding keeps a running average that ignores trade dates. A purchase dated
    // before a sale would change what that sale "cost", so the holding and the
    // performance replay (which sorts by date) would disagree. Reject that case.
    private void requireNoLaterSale(Long portfolioId, String symbol, Instant executedAt) {
        tradeRepository.findAllByPortfolioId(portfolioId).stream()
                .filter(t -> t.getTicker().equals(symbol) && t.getType() == TradeType.SELL)
                .map(Trade::getExecutedAt)
                .filter(soldAt -> soldAt.isAfter(executedAt))
                .max(Comparator.naturalOrder())
                .ifPresent(soldAt -> {
                    throw new IllegalArgumentException("You sold " + symbol + " after that date (" + soldAt
                            + "). Purchases can only be recorded after your most recent sale.");
                });
    }

    @Transactional
    public Holding sell(Long portfolioId, String userId, String ticker, BigDecimal quantity) {
        String symbol = ticker.toUpperCase();
        requireOwnedPortfolio(portfolioId, userId);

        BigDecimal price = priceClient.getPrice(symbol);

        Holding holding = holdingRepository.findByPortfolioIdAndTicker(portfolioId, symbol)
                .orElseThrow(() -> new InsufficientSharesException(symbol, quantity, BigDecimal.ZERO));


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