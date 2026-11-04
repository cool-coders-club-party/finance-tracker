package ie.universityofgalway.finance.portfolio.application.service;

import ie.universityofgalway.finance.portfolio.application.port.PortfolioRepository;
import ie.universityofgalway.finance.portfolio.application.port.PriceClient;
import ie.universityofgalway.finance.portfolio.application.port.TradeRepository;
import ie.universityofgalway.finance.portfolio.domain.exception.PortfolioNotFoundException;
import ie.universityofgalway.finance.portfolio.domain.model.HoldingPerformance;
import ie.universityofgalway.finance.portfolio.domain.model.Trade;
import ie.universityofgalway.finance.portfolio.domain.service.PerformanceCalculator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class PerformanceService {

    private final PortfolioRepository portfolioRepository;
    private final TradeRepository tradeRepository;
    private final PriceClient priceClient;

    public PerformanceService(PortfolioRepository portfolioRepository,
                              TradeRepository tradeRepository,
                              PriceClient priceClient) {
        this.portfolioRepository = portfolioRepository;
        this.tradeRepository = tradeRepository;
        this.priceClient = priceClient;
    }

    public List<HoldingPerformance> getPerformance(Long portfolioId, String userId) {
        // Ownership check: someone else's portfolio looks identical to a missing one (404).
        portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        List<Trade> trades = tradeRepository
                .findAllByPortfolioId(portfolioId);

        // TreeMap = sorted by ticker; each list keeps the oldest-first order.
        Map<String, List<Trade>> byTicker = trades.stream()
                .collect(Collectors.groupingBy(Trade::getTicker, TreeMap::new, Collectors.toList()));

        List<HoldingPerformance> result = new ArrayList<>();
        for (Map.Entry<String, List<Trade>> entry : byTicker.entrySet()) {
            String ticker = entry.getKey();
            List<Trade> tickerTrades = entry.getValue();

            boolean open = PerformanceCalculator.netQuantity(tickerTrades).signum() > 0;
            BigDecimal price = open ? priceClient.getPrice(ticker) : null;

            result.add(PerformanceCalculator.calculate(ticker, tickerTrades, price));
        }
        return result;
    }
}
