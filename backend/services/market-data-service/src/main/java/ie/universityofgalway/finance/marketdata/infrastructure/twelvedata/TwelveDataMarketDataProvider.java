package ie.universityofgalway.finance.marketdata.infrastructure.twelvedata;

import ie.universityofgalway.finance.marketdata.domain.QuoteDetails;
import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import ie.universityofgalway.finance.marketdata.domain.Quote;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Retrieves live quotes from Twelve Data when the local profile is active.
 *
 * <p>HTTP failures other than 404 propagate to the quote-cache decorator.</p>
 */
@Component("liveMarketDataProvider")
@Profile("local")
public class TwelveDataMarketDataProvider implements MarketDataProvider {

    private final RestClient restClient;

    /**
     * Builds an HTTP client using the configured base URL and API-key header.
     *
     * @param restClientBuilder builder for the provider HTTP client
     * @param properties Twelve Data endpoint and API key
     */
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

    /**
     * Maps a Twelve Data quote into the domain model.
     *
     * <p>Missing required fields or HTTP 404 yield no quote. Missing or malformed optional
     * statistics become null without hiding a usable price.</p>
     *
     * @param symbol normalised ticker sent to the quote endpoint
     * @return the mapped quote, or an empty optional when required quote data is absent
     * @throws org.springframework.web.client.RestClientException if the HTTP request fails other than with
     *     404
     * @throws NumberFormatException if the required closing price is not a decimal number
     */
    @Override
    public Optional<Quote> findQuote(String symbol) {
        TwelveDataQuoteResponse response;

        try {
            // Request the latest quote for the supplied ticker.
            response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbol", symbol)
                            .build())
                    .retrieve()
                    .body(TwelveDataQuoteResponse.class);
        } catch (HttpClientErrorException exception) {
            // An unknown ticker is a normal "not found" result.
            if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                return Optional.empty();
            }

            // Authentication and rate-limit errors are real provider failures.
            throw exception;
        }

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

        TwelveDataQuoteResponse.FiftyTwoWeek range = response.fiftyTwoWeek();

        QuoteDetails details = new QuoteDetails(
                response.name(),
                response.currency(),
                parseOptionalDecimal(response.open()),
                parseOptionalDecimal(response.high()),
                parseOptionalDecimal(response.low()),
                parseOptionalLong(response.volume()),
                parseOptionalDecimal(response.previousClose()),
                parseOptionalDecimal(response.change()),
                parseOptionalDecimal(response.percentChange()),
                range == null ? null : parseOptionalDecimal(range.low()),
                range == null ? null : parseOptionalDecimal(range.high())
        );

        Quote quote = new Quote(
                response.symbol(),
                price,
                asOf,
                false,
                details
        );

        return Optional.of(quote);
    }

    // A missing or malformed optional value should not hide the price.
    /**
     * Parses an optional decimal without rejecting the rest of the quote.
     *
     * @param value provider text; may be null, blank or malformed
     * @return the decimal value, or null if it cannot be parsed
     */
    private BigDecimal parseOptionalDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Parses optional volume without rejecting the rest of the quote.
     *
     * @param value provider text; may be null, blank or malformed
     * @return the integer value, or null if it cannot be parsed
     */
    private Long parseOptionalLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

}
