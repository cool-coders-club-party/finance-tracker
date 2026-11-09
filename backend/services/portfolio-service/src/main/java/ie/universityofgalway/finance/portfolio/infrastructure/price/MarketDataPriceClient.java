package ie.universityofgalway.finance.portfolio.infrastructure.price;

import ie.universityofgalway.finance.portfolio.application.port.PriceClient;
import ie.universityofgalway.finance.portfolio.domain.exception.InvalidSymbolException;
import ie.universityofgalway.finance.portfolio.domain.exception.PriceUnavailableException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "price-client.type", havingValue = "market-data")
public class MarketDataPriceClient implements PriceClient {

    private record QuoteDto(String symbol, BigDecimal price) {
    }

    private final RestClient restClient;

    public MarketDataPriceClient(RestClient marketDataRestClient) {
        this.restClient = marketDataRestClient;
    }

    @Override
    public BigDecimal getPrice(String ticker) {
        try {
            // {symbol} is URL-encoded for us, so odd characters can't change the path
            QuoteDto quote = restClient.get()
                    .uri("/api/quotes/{symbol}", ticker)
                    .retrieve()
                    .body(QuoteDto.class);

            if (quote == null || quote.price() == null) {
                throw new PriceUnavailableException(
                        "Market Data returned no price for " + ticker, null);
            }
            return quote.price();

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

