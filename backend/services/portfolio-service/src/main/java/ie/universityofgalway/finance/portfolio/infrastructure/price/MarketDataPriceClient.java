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

@Component
@ConditionalOnProperty(name = "price-client.type", havingValue = "market-data")
public class MarketDataPriceClient implements PriceClient {

    private record DetailsDto(String currency) {}

    private record QuoteDto(String symbol, BigDecimal price, Instant asOf, Boolean stale, DetailsDto details) {
    }

    private final RestClient restClient;

    public MarketDataPriceClient(RestClient marketDataRestClient) {
        this.restClient = marketDataRestClient;
    }

    @Override
    public BigDecimal getPrice(String ticker) {
        return getQuote(ticker).price();
    }

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

