package ie.universityofgalway.finance.marketdata.infrastructure.twelvedata;

import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import ie.universityofgalway.finance.marketdata.domain.Quote;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Component
@Profile("local")
public class TwelveDataMarketDataProvider implements MarketDataProvider {

    private final RestClient restClient;

    public TwelveDataMarketDataProvider(
            RestClient.Builder restClientBuilder,
            TwelveDataProperties properties
    ) {
        // Configure one HTTP client for all Twelve Data requests.
        restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "apikey " + properties.apiKey()
                )
                .build();
    }

    @Override
    public Optional<Quote> findQuote(String symbol) {
        // Request the latest quote for the supplied ticker.
        TwelveDataQuoteResponse response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/quote")
                        .queryParam("symbol", symbol)
                        .build())
                .retrieve()
                .body(TwelveDataQuoteResponse.class);

        // Missing fields mean that no usable quote was returned.
        if (response == null
                || response.symbol() == null
                || response.close() == null
                || response.timestamp() == null) {
            return Optional.empty();
        }

        // Convert provider-specific values into the domain model.
        BigDecimal price = new BigDecimal(response.close());
        Instant asOf = Instant.ofEpochSecond(response.timestamp());

        Quote quote = new Quote(
                response.symbol(),
                price,
                asOf
        );

        return Optional.of(quote);
    }
}