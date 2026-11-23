package ie.universityofgalway.finance.portfolio.application.service;

import ie.universityofgalway.finance.portfolio.application.port.PriceClient;
import ie.universityofgalway.finance.portfolio.domain.exception.InvalidSymbolException;
import ie.universityofgalway.finance.portfolio.domain.exception.PriceUnavailableException;
import ie.universityofgalway.finance.portfolio.domain.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Values saved holdings using market snapshots without changing recorded purchase costs.
 *
 * <p>Only matching USD purchase and quote currencies are supported. When any holding
 * is unavailable, all portfolio money totals are null rather than misleading partial sums.</p>
 */
@Service
public class ValuationService {
    private final PortfolioService portfolios;
    private final PriceClient prices;

    /**
     * Creates the valuation service.
     *
     * @param portfolios service used to authorise access and retrieve holdings
     * @param prices client supplying market snapshots
     */
    public ValuationService(PortfolioService portfolios, PriceClient prices) {
        this.portfolios = portfolios;
        this.prices = prices;
    }

    /**
     * Authorises the portfolio before requesting quotes, then calculates unrounded decimal values.
     *
     * <p>Quote requests occur after the holdings read, outside its database transaction.
     * Expected quote failures are represented as unavailable holdings rather than failing the whole
     * response.</p>
     *
     * @param portfolioId portfolio to value
     * @param userId authenticated owner identifier
     * @return per-holding availability and USD totals only when all holdings are available
     * @throws ie.universityofgalway.finance.portfolio.domain.exception.PortfolioNotFoundException if the
     *     portfolio does not exist or is not owned by the user
     */
    public PortfolioValuation getValuation(Long portfolioId, String userId) {
        // Read and authorise before making network calls; no database transaction spans quote requests.
        List<HoldingValuation> holdings = portfolios.getHoldings(portfolioId, userId).stream()
                .map(this::value).toList();
        int unavailable = (int) holdings.stream().filter(h -> !h.available()).count();
        boolean complete = unavailable == 0;
        boolean stale = holdings.stream().anyMatch(h -> h.quote() != null && h.quote().stale());
        // Never present a subtotal as the whole portfolio's value.
        return new PortfolioValuation(holdings, "USD", complete, stale, unavailable,
                complete ? sum(holdings, HoldingValuation::costBasis) : null,
                complete ? sum(holdings, HoldingValuation::currentValue) : null,
                complete ? sum(holdings, HoldingValuation::unrealisedGainLoss) : null);
    }

    /**
     * Checks quote identity, positive price, timestamp and matching USD currencies before valuation.
     *
     * <p>Failures use QUOTE_UNAVAILABLE, INVALID_QUOTE, CURRENCY_MISSING or CURRENCY_UNSUPPORTED.</p>
     *
     * @param holding holding to value
     * @return the calculated amounts or an unavailable result with a reason code
     */
    private HoldingValuation value(Holding holding) {
        MarketQuote quote;
        try {
            quote = prices.getQuote(holding.getTicker());
        } catch (PriceUnavailableException | InvalidSymbolException e) {
            return unavailable(holding, null, "QUOTE_UNAVAILABLE");
        }
        // Check again at the application boundary, so every PriceClient must supply usable metadata.
        if (quote == null || quote.price() == null || quote.price().signum() <= 0
                || !holding.getTicker().equalsIgnoreCase(quote.symbol()) || quote.asOf() == null) {
            return unavailable(holding, quote, "INVALID_QUOTE");
        }
        if (holding.getCurrency() == null || holding.getCurrency().isBlank()
                || quote.currency() == null || quote.currency().isBlank()) {
            return unavailable(holding, quote, "CURRENCY_MISSING");
        }
        // USD only until an explicit currency-conversion policy exists.
        if (!"USD".equals(holding.getCurrency()) || !holding.getCurrency().equals(quote.currency())) {
            return unavailable(holding, quote, "CURRENCY_UNSUPPORTED");
        }
        BigDecimal cost = holding.getQuantity().multiply(holding.getAvgPurchasePrice());
        BigDecimal current = holding.getQuantity().multiply(quote.price());
        return new HoldingValuation(holding, quote, cost, current, current.subtract(cost), null);
    }

    /**
     * Builds an unavailable result while retaining any quote metadata for display.
     *
     * @param holding holding that could not be valued
     * @param quote quote received, or null if the lookup failed
     * @param reason machine-readable reason code
     * @return a valuation with null money amounts
     */
    private HoldingValuation unavailable(Holding holding, MarketQuote quote, String reason) {
        return new HoldingValuation(holding, quote, null, null, null, reason);
    }

    /**
     * Sums one monetary field after all holding valuations have been confirmed available.
     *
     * @param holdings available holding valuations
     * @param field non-null monetary field to aggregate
     * @return the exact sum, or zero for no holdings
     */
    private BigDecimal sum(List<HoldingValuation> holdings,
                           java.util.function.Function<HoldingValuation, BigDecimal> field) {
        return holdings.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
