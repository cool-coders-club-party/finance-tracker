package ie.universityofgalway.finance.portfolio.infrastructure.price;

import ie.universityofgalway.finance.portfolio.application.port.PriceClient;
import ie.universityofgalway.finance.portfolio.domain.exception.InvalidSymbolException;
import ie.universityofgalway.finance.portfolio.domain.exception.PriceUnavailableException;
import ie.universityofgalway.finance.portfolio.domain.model.MarketQuote;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Obtains Portfolio prices from the Market Data service when {@code price-client.type=market-data}.
 *
 * <p>Transport errors and unusable quote metadata are translated into domain failures.</p>
 */
@Component
@ConditionalOnProperty(name = "price-client.type", havingValue = "market-data")
public class MarketDataPriceClient implements PriceClient {

    /**
     * Currency metadata read from a Market Data quote.
     *
     * @param currency provider currency code; may be null
     */
    private record DetailsDto(String currency) {}

    /**
     * Internal JSON shape for a Market Data quote.
     *
     * @param symbol returned stock symbol
     * @param price unit price
     * @param asOf provider price timestamp
     * @param stale boxed stale flag so a missing field can be rejected
     * @param details optional currency metadata
     */
    private record QuoteDto(String symbol, BigDecimal price, Instant asOf, Boolean stale, DetailsDto details) {
    }

    private final RestClient restClient;

    /**
     * Creates the adapter with a preconfigured Market Data HTTP client.
     *
     * @param marketDataRestClient client containing the Market Data base URL and request settings
     */
    public MarketDataPriceClient(RestClient marketDataRestClient) {
        this.restClient = marketDataRestClient;
    }

    /**
     * Retrieves a validated quote and returns its unit price.
     *
     * @param ticker stock symbol to request
     * @return the quoted unit price
     * @throws ie.universityofgalway.finance.portfolio.domain.exception.InvalidSymbolException if the provider
     *     does not recognise the ticker
     * @throws PriceUnavailableException if the provider cannot supply a usable price
     */
    @Override
    public BigDecimal getPrice(String ticker) {
        return getQuote(ticker).price();
    }

    /**
     * Requests an encoded ticker path and validates quote identity, price, timestamp and stale flag.
     *
     * <p>Missing currency is preserved as null so the valuation service can report it explicitly.</p>
     *
     * @param ticker non-null stock symbol to request
     * @return the validated quote with optional currency metadata
     * @throws InvalidSymbolException if Market Data returns HTTP 404
     * @throws PriceUnavailableException if the service fails or required quote metadata is unusable
     */
    @Override
    public MarketQuote getQuote(String ticker) {
        try {
            // {symbol} is URL-encoded for us, so odd characters can't change the path
            QuoteDto quote = restClient.get()
                    .uri("/api/quotes/{symbol}", ticker)
                    .retrieve()
                    .body(QuoteDto.class);

            if (quote == null || quote.price() == null || quote.price().signum() <= 0
                    || !ticker.equalsIgnoreCase(quote.symbol()) || quote.asOf() == null || quote.stale() == null) {
                throw new PriceUnavailableException(
                        "Market Data returned an invalid quote for " + ticker, null);
            }
            return new MarketQuote(quote.symbol(), quote.price(),
                    quote.details() == null ? null : quote.details().currency(), quote.asOf(), quote.stale());

        } catch (HttpClientErrorException.NotFound e) {
            // Market Data says the ticker doesn't exist: same as the fake client did
            throw new InvalidSymbolException(ticker);

        } catch (RestClientException e) {
            // Down, timed out, 500, bad JSON: not the user's fault
            throw new PriceUnavailableException(
                    "Could not get a price for " + ticker + " from Market Data", e);
        }
    }
}

