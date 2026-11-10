package ie.universityofgalway.finance.marketdata.application;

import ie.universityofgalway.finance.marketdata.domain.MarketDataUnavailableException;
import ie.universityofgalway.finance.marketdata.domain.Quote;
import ie.universityofgalway.finance.marketdata.domain.QuoteDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockMoversServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-10T16:00:00Z");
    private final QuoteService quoteService = mock(QuoteService.class);
    private final Clock clock = mock(Clock.class);
    private final StockMoversService service = new StockMoversService(quoteService, clock);

    @BeforeEach
    void setClock() {
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    void ranksByMagnitudeThenTickerAndKeepsOnlyFiveRows() {
        stubQuote("AAPL", "8", false);
        stubQuote("MSFT", "-8", false);
        stubQuote("NVDA", "-12", false);
        stubQuote("AMZN", "5", false);
        stubQuote("GOOGL", "3", false);
        stubQuote("META", "0", false);
        stubQuote("TSLA", null, false);

        var ranking = service.getMovers().orElseThrow();

        assertThat(ranking.trackedCount()).isEqualTo(8);
        assertThat(ranking.usableCount()).isEqualTo(6);
        assertThat(ranking.rows()).extracting(StockMoversService.MoverRow::ticker)
                .containsExactly("NVDA", "AAPL", "MSFT", "AMZN", "GOOGL");
        assertThat(ranking.rows().getFirst().percentChange()).isEqualByComparingTo("-12");
    }

    @Test
    void preservesStaleQuoteDetailsWhenAnotherTickerFails() {
        stubQuote("AAPL", "-2", true);
        when(quoteService.getQuote("MSFT")).thenThrow(new MarketDataUnavailableException(null));

        var ranking = service.getMovers().orElseThrow();

        assertThat(ranking.rows()).singleElement().satisfies(row -> {
            assertThat(row.company()).isEqualTo("Apple");
            assertThat(row.asOf()).isEqualTo(NOW);
            assertThat(row.stale()).isTrue();
        });
    }

    @Test
    void returnsEmptyRowsWhenQuotesLackDailyChanges() {
        stubQuote("AAPL", null, false);

        assertThat(service.getMovers().orElseThrow().rows()).isEmpty();
    }

    @Test
    void reusesRankingUntilFreshnessWindowEnds() {
        stubQuote("AAPL", "4", false);
        var first = service.getMovers();

        when(clock.instant()).thenReturn(NOW.plusSeconds(899));
        assertThat(service.getMovers()).isSameAs(first);
        verify(quoteService).getQuote("AAPL");

        when(clock.instant()).thenReturn(NOW.plusSeconds(900));
        service.getMovers();
        verify(quoteService, times(2)).getQuote("AAPL");
    }

    @Test
    void coolsDownBeforeRetryingAnUnavailableRanking() {
        when(quoteService.getQuote("AAPL")).thenThrow(new MarketDataUnavailableException(null));
        assertThat(service.getMovers()).isEmpty();

        when(clock.instant()).thenReturn(NOW.plusSeconds(59));
        assertThat(service.getMovers()).isEmpty();
        verify(quoteService).getQuote("AAPL");

        when(clock.instant()).thenReturn(NOW.plusSeconds(60));
        assertThat(service.getMovers()).isEmpty();
        verify(quoteService, times(2)).getQuote("AAPL");
    }

    private void stubQuote(String ticker, String percentChange, boolean stale) {
        var details = new QuoteDetails(null, "USD", null, null, null, null, null, null,
                percentChange == null ? null : new BigDecimal(percentChange), null, null);
        var quote = new Quote(ticker, BigDecimal.TEN, NOW, stale, details);
        when(quoteService.getQuote(ticker)).thenReturn(Optional.of(quote));
    }
}
