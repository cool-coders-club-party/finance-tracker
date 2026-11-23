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

/**
 * Application service for portfolios, buying and selling.
 *
 * <p>Holds the business rules for trades:
 * <ul>
 *   <li>Every operation first checks the portfolio belongs to the caller; someone else's
 *       portfolio is reported as not found.</li>
 *   <li>A buy or sell updates the holding and records the trade in one transaction.</li>
 *   <li>A buy uses the live market price, unless it records an earlier purchase at the
 *       price actually paid.</li>
 *   <li>A new holding takes its currency from the market quote.</li>
 * </ul>
 *
 * <p>It has no knowledge of HTTP; {@code PortfolioController} calls it and turns its results
 * and exceptions into responses.</p>
 */
@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final TradeRepository tradeRepository;
    private final PriceClient priceClient;

    /**
     * @param portfolioRepository storage for portfolios, also used for ownership checks
     * @param holdingRepository   storage for holdings
     * @param tradeRepository     storage for trades
     * @param priceClient         source of market prices and currencies
     */
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

    /**
     * Buys shares at the live market price, timestamped now.
     *
     * @param portfolioId the portfolio receiving the shares
     * @param userId      the caller's user id, from their access token
     * @param ticker      the stock symbol, in any case
     * @param quantity    how many shares; must be greater than zero
     * @return the updated holding
     * @see #buy(Long, String, String, BigDecimal, BigDecimal, Instant)
     */
    @Transactional
    public Holding buy(Long portfolioId, String userId, String ticker, BigDecimal quantity) {
        return buy(portfolioId, userId, ticker, quantity, null, null);
    }

    /**
     * Buys shares, or records a purchase made earlier when {@code pricePaid} and
     * {@code executedAt} are both given (e.g. bought yesterday, entered today).
     *
     * <p>The market quote is fetched even for an earlier purchase: it confirms the ticker
     * exists and supplies the currency for a new holding. The new average price is weighted
     * by quantity across all purchases.</p>
     *
     * @param portfolioId the portfolio receiving the shares
     * @param userId      the caller's user id, from their access token
     * @param ticker      the stock symbol, in any case; stored upper case
     * @param quantity    how many shares; must be greater than zero
     * @param pricePaid   price per share actually paid, or {@code null} to buy at market price
     * @param executedAt  when the earlier purchase happened, or {@code null} for now
     * @return the updated holding
     * @throws IllegalArgumentException if only one of {@code pricePaid} and {@code executedAt}
     *         is given, the price isn't positive, the date is in the future (beyond one minute of
     *         clock difference), or the date is before the most recent sale of the same stock
     * @throws PortfolioNotFoundException if the portfolio doesn't exist or isn't the caller's
     * @throws ie.universityofgalway.finance.portfolio.domain.exception.InvalidSymbolException
     *         if market data doesn't recognise the ticker
     * @throws ie.universityofgalway.finance.portfolio.domain.exception.PriceUnavailableException
     *         if market data can't be reached
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

    /**
     * Rejects a backdated purchase dated before an existing sale of the same stock.
     *
     * <p>The holding keeps a running average that ignores trade dates. A purchase dated
     * before a sale would change what that sale "cost", so the holding and the performance
     * replay (which sorts by date) would disagree.</p>
     *
     * @param portfolioId the portfolio id
     * @param symbol      the stock symbol, upper case
     * @param executedAt  the proposed purchase time
     * @throws IllegalArgumentException if a sale of {@code symbol} happened after {@code executedAt}
     */
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

    /**
     * Sells shares at the live market price, timestamped now.
     *
     * <p>Selling every share leaves the holding in place with a quantity of zero, so its trade
     * history keeps its link. The average purchase price is unchanged by a sale.</p>
     *
     * @param portfolioId the portfolio holding the shares
     * @param userId      the caller's user id, from their access token
     * @param ticker      the stock symbol, in any case
     * @param quantity    how many shares; must be greater than zero
     * @return the updated holding
     * @throws InsufficientSharesException if the portfolio holds fewer shares, or none
     * @throws PortfolioNotFoundException  if the portfolio doesn't exist or isn't the caller's
     * @throws ie.universityofgalway.finance.portfolio.domain.exception.PriceUnavailableException
     *         if market data can't be reached
     */
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

    /**
     * Lists a user's portfolios.
     *
     * @param userId the caller's user id, from their access token
     * @return the portfolios, or an empty list
     */
    @Transactional(readOnly = true)
    public List<Portfolio> getPortfolios(String userId) {
        return portfolioRepository.findByUserId(userId);
    }

    /**
     * Lists the holdings in one of the caller's portfolios.
     *
     * @param portfolioId the portfolio id
     * @param userId      the caller's user id, from their access token
     * @return the holdings, including any sold down to zero shares
     * @throws PortfolioNotFoundException if the portfolio doesn't exist or isn't the caller's
     */
    @Transactional(readOnly = true)
    public List<Holding> getHoldings(Long portfolioId, String userId) {
        requireOwnedPortfolio(portfolioId, userId);
        return holdingRepository.findByPortfolioId(portfolioId);
    }

    /**
     * Lists every trade in one of the caller's portfolios.
     *
     * @param portfolioId the portfolio id
     * @param userId      the caller's user id, from their access token
     * @return the trades, oldest first
     * @throws PortfolioNotFoundException if the portfolio doesn't exist or isn't the caller's
     */
    @Transactional(readOnly = true)
    public List<Trade> getTrades(Long portfolioId, String userId) {
        requireOwnedPortfolio(portfolioId, userId);
        return tradeRepository.findAllByPortfolioId(portfolioId);
    }

    /**
     * Creates a portfolio owned by the given user.
     *
     * @param userId the caller's user id, from their access token
     * @param name   the display name; surrounding spaces are removed
     * @return the saved portfolio, with its id
     * @throws IllegalArgumentException if the name is blank
     */
    @Transactional
    public Portfolio createPortfolio(String userId, String name) {
        return portfolioRepository.save(Portfolio.create(userId, name));
    }

    /**
     * The ownership check run before every portfolio operation.
     *
     * @param portfolioId the portfolio id
     * @param userId      the caller's user id
     * @throws PortfolioNotFoundException if the portfolio doesn't exist or belongs to someone
     *         else; the two cases look the same, so ids can't be probed
     */
    private void requireOwnedPortfolio(Long portfolioId, String userId) {
        portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
    }
}