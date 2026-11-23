package ie.universityofgalway.finance.portfolio.infrastructure.price;

import ie.universityofgalway.finance.portfolio.application.port.PriceClient;
import ie.universityofgalway.finance.portfolio.domain.exception.InvalidSymbolException;
import ie.universityofgalway.finance.portfolio.domain.model.MarketQuote;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * {@link PriceClient} with fixed USD prices, for running without market-data-service.
 *
 * <p>Active only when {@code price-client.type=fake} (the shipped configuration uses
 * {@code market-data}), and never under the {@code prod} profile. Only {@code AAPL} and
 * {@code MSFT} have prices; any other ticker is treated as unknown.</p>
 *
 * <p>Nothing else depends on this class directly - callers only depend on the
 * {@link PriceClient} interface, so the real and fake clients can be swapped by configuration
 * without changing any service or controller code.</p>
 */
@Component
@Profile("!prod") // guards against this ever being wired into a real environment
@ConditionalOnProperty(name = "price-client.type", havingValue = "fake", matchIfMissing = true)
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

    /**
     * {@inheritDoc}
     *
     * <p>Always returns a fresh USD quote timestamped now. Buying needs the currency as well
     * as the price.</p>
     */
    @Override
    public MarketQuote getQuote(String ticker) {
        return new MarketQuote(ticker.toUpperCase(), getPrice(ticker), "USD", Instant.now(), false);
    }
}
