package ie.universityofgalway.finance.marketdata.infrastructure;

import ie.universityofgalway.finance.marketdata.domain.Quote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StubMarketDataProviderTest {

    @Test
    void returnsPriceForKnownSymbol() {

        StubMarketDataProvider provider = new StubMarketDataProvider();

        Optional<Quote> result = provider.findQuote("AAPL");

        assertTrue(result.isPresent());

        Quote quote = result.get();

        assertEquals("AAPL", quote.symbol());
        assertEquals(new BigDecimal("192.45"), quote.price());
    }

    @Test
    void handlesMessyInput() {

        StubMarketDataProvider provider = new StubMarketDataProvider();

        Optional<Quote> result = provider.findQuote("  aapl  ");

        assertTrue(result.isPresent());
        assertEquals("AAPL", result.get().symbol());
    }

    @Test
    void returnsNothingForUnknownSymbol() {

        StubMarketDataProvider provider = new StubMarketDataProvider();

        Optional<Quote> result = provider.findQuote("NOTAREALSTOCK");

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsNothingForNull() {

        StubMarketDataProvider provider = new StubMarketDataProvider();

        Optional<Quote> result = provider.findQuote(null);

        assertTrue(result.isEmpty());
    }
}