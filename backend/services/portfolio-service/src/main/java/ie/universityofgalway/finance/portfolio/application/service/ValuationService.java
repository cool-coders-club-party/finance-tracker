package ie.universityofgalway.finance.portfolio.application.service;

import ie.universityofgalway.finance.portfolio.application.port.PriceClient;
import ie.universityofgalway.finance.portfolio.domain.exception.InvalidSymbolException;
import ie.universityofgalway.finance.portfolio.domain.exception.PriceUnavailableException;
import ie.universityofgalway.finance.portfolio.domain.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ValuationService {
    private final PortfolioService portfolios;
    private final PriceClient prices;

    public ValuationService(PortfolioService portfolios, PriceClient prices) {
        this.portfolios = portfolios;
        this.prices = prices;
    }

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

    private HoldingValuation unavailable(Holding holding, MarketQuote quote, String reason) {
        return new HoldingValuation(holding, quote, null, null, null, reason);
    }

    private BigDecimal sum(List<HoldingValuation> holdings,
                           java.util.function.Function<HoldingValuation, BigDecimal> field) {
        return holdings.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
