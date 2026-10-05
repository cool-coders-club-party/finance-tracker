package ie.universityofgalway.finance.portfolio.price;

import ie.universityofgalway.finance.portfolio.exception.InvalidSymbolException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Temporary stand-in for the real market-data provider.
 *
 * Nothing else in the codebase depends on this class directly - callers only
 * depend on the PriceClient interface, so swapping in the real provider later
 * is just a matter of adding a new @Component implementation and removing
 * (or re-scoping) this one. No service or controller code needs to change.
 */
@Component
@Profile("!prod") // guards against this ever being wired into a real environment
public class FakePriceClient implements PriceClient {

    private static final Map<String, BigDecimal> PRICES = Map.of(
            "AAPL", new BigDecimal("192.45"),
            "MSFT", new BigDecimal("415.20")
    );

    @Override
    public BigDecimal getPrice(String ticker) {
        BigDecimal price = PRICES.get(ticker.toUpperCase());
        if (price == null) {
            throw new InvalidSymbolException(ticker);
        }
        return price;
    }
}
