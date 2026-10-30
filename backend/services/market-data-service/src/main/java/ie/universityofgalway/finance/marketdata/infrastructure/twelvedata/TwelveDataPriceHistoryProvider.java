package ie.universityofgalway.finance.marketdata.infrastructure.twelvedata;

import ie.universityofgalway.finance.marketdata.domain.MarketDataUnavailableException;
import ie.universityofgalway.finance.marketdata.domain.PriceHistoryProvider;
import ie.universityofgalway.finance.marketdata.domain.PricePoint;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@Profile("local")
public class TwelveDataPriceHistoryProvider implements PriceHistoryProvider {
    private final RestClient restClient;
    private final Clock clock;

    public TwelveDataPriceHistoryProvider(
            RestClient.Builder builder,
            TwelveDataProperties properties,
            Clock clock
    ) {
        // Use the same API configuration as the live quote provider.
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "apikey " + properties.apiKey())
                .build();
        this.clock = clock;
    }

    @Override
    public Optional<List<PricePoint>> findDailyHistory(String symbol) {
        // Request one calendar month of daily prices.
        LocalDate end = LocalDate.now(clock);
        LocalDate start = end.minusMonths(1);
        TwelveDataTimeSeriesResponse response;

        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/time_series")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", "1day")
                            .queryParam("start_date", start)
                            .queryParam("end_date", end)
                            .build())
                    .retrieve()
                    .body(TwelveDataTimeSeriesResponse.class);
        } catch (HttpClientErrorException exception) {
            // An unknown symbol has no history.
            if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                return Optional.empty();
            }
            throw new MarketDataUnavailableException(exception);
        } catch (RestClientException exception) {
            // Network and other provider failures become a clear service error.
            throw new MarketDataUnavailableException(exception);
        }

        // A missing or error response cannot produce a reliable graph.
        if (response == null || "error".equals(response.status()) || response.values() == null) {
            throw new MarketDataUnavailableException(
                    new IllegalStateException("Invalid time series response")
            );
        }

        if (response.values().isEmpty()) {
            return Optional.empty();
        }

        try {
            // Twelve Data sends newest first; graphs need oldest first.
            List<PricePoint> points = response.values().stream()
                    .map(value -> new PricePoint(
                            LocalDate.parse(value.datetime()),
                            new BigDecimal(value.close())
                    ))
                    .sorted(Comparator.comparing(PricePoint::date))
                    .toList();

            return Optional.of(points);
        } catch (RuntimeException exception) {
            // Bad dates or prices should not become a broken graph.
            throw new MarketDataUnavailableException(exception);
        }
    }
}