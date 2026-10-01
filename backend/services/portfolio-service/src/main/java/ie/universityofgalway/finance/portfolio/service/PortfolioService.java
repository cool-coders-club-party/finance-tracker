package ie.universityofgalway.finance.portfolio.service;

import ie.universityofgalway.finance.portfolio.exception.InsufficientSharesException;
import ie.universityofgalway.finance.portfolio.model.Holding;
import ie.universityofgalway.finance.portfolio.model.Portfolio;
import ie.universityofgalway.finance.portfolio.model.Trade;
import ie.universityofgalway.finance.portfolio.model.TradeType;
import ie.universityofgalway.finance.portfolio.price.PriceClient;
import ie.universityofgalway.finance.portfolio.repository.HoldingRepository;
import ie.universityofgalway.finance.portfolio.repository.PortfolioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final PriceClient priceClient;

    public PortfolioService(PortfolioRepository portfolioRepository,
                             HoldingRepository holdingRepository,
                             PriceClient priceClient) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.priceClient = priceClient;
    }

    @Transactional
    public Holding buy(Long portfolioId, String userId, String ticker, BigDecimal quantity) {
        requirePositive(quantity);
        String symbol = ticker.toUpperCase();

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found"));

        // Validate the symbol (and get the fill price) before touching any state.
        // FakePriceClient throws InvalidSymbolException for anything not in its map.
        BigDecimal price = priceClient.getPrice(symbol);

        Holding holding = holdingRepository.findByPortfolioIdAndTicker(portfolioId, symbol)
                .orElseGet(() -> {
                    Holding h = new Holding(portfolio, symbol);
                    portfolio.addHolding(h);
                    return h;
                });

        // Weighted average: blend existing position value with the new purchase.
        BigDecimal existingValue = holding.getQuantity().multiply(holding.getAvgPurchasePrice());
        BigDecimal purchaseValue = quantity.multiply(price);
        BigDecimal newQuantity = holding.getQuantity().add(quantity);
        BigDecimal newAvgPrice = existingValue.add(purchaseValue)
                .divide(newQuantity, 4, RoundingMode.HALF_UP);

        holding.setQuantity(newQuantity);
        holding.setAvgPurchasePrice(newAvgPrice);
        holding.addTrade(new Trade(holding, TradeType.BUY, quantity, price, Instant.now()));

        return holdingRepository.save(holding);
    }

    @Transactional
    public Holding sell(Long portfolioId, String userId, String ticker, BigDecimal quantity) {
        requirePositive(quantity);
        String symbol = ticker.toUpperCase();

        portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found"));

        Holding holding = holdingRepository.findByPortfolioIdAndTicker(portfolioId, symbol)
                .orElseThrow(() -> new InsufficientSharesException(symbol, quantity, BigDecimal.ZERO));

        // Hard stop: can never sell more than is currently held.
        if (holding.getQuantity().compareTo(quantity) < 0) {
            throw new InsufficientSharesException(symbol, quantity, holding.getQuantity());
        }

        BigDecimal price = priceClient.getPrice(symbol);

        holding.setQuantity(holding.getQuantity().subtract(quantity));
        // avgPurchasePrice is deliberately left unchanged on a sell - cost basis
        // of the remaining shares doesn't move just because some were sold.
        holding.addTrade(new Trade(holding, TradeType.SELL, quantity, price, Instant.now()));

        return holdingRepository.save(holding);
    }

    private void requirePositive(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }
}
