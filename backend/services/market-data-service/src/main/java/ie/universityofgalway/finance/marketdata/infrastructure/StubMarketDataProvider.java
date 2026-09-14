package ie.universityofgalway.finance.marketdata.infrastructure;

import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import ie.universityofgalway.finance.marketdata.domain.Quote;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Profile("stub")
public class StubMarketDataProvider implements MarketDataProvider {

    private static final Map<String, BigDecimal> PRICES = new HashMap<>();

    //filling the PRICES HashMap
    static {
        PRICES.put("AAPL", new BigDecimal("192.45"));
        PRICES.put("MSFT", new BigDecimal("415.20"));
        PRICES.put("TSLA", new BigDecimal("248.90"));
    }

    @Override
    public Optional<Quote> findQuote(String symbol) {

        if (symbol == null) {
            return Optional.empty();
        }

        String key = symbol.trim().toUpperCase();

        BigDecimal price = PRICES.get(key);

        if (price == null) {
            return Optional.empty();
        }

        Quote quote = new Quote(key, price, Instant.now());

        return Optional.of(quote);
    }


}
