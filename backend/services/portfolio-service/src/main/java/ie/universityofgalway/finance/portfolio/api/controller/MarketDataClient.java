package ie.universityofgalway.finance.portfolio.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configures the HTTP client used to call market-data-service.
 *
 * <p>Only active when {@code price-client.type=market-data}. The short timeouts make a slow or
 * unreachable market-data-service fail fast (as {@code 503 PRICE_UNAVAILABLE}) instead of
 * holding the user's request open.</p>
 *
 * <p>Despite its package, this is configuration, not a controller; it is used by
 * {@link ie.universityofgalway.finance.portfolio.infrastructure.price.MarketDataPriceClient}.</p>
 */
@Configuration
@ConditionalOnProperty(name = "price-client.type", havingValue = "market-data")
public class MarketDataClient {
    /**
     * Builds the {@link RestClient} for market-data-service.
     *
     * @param baseURL          market-data address, {@code market-data.base-url}
     *                         (e.g. {@code http://localhost:8084})
     * @param connectTimeoutMs how long to wait for a connection, {@code market-data.connect-timeout-ms}
     * @param readTimeoutMs    how long to wait for a response, {@code market-data.read-timeout-ms}
     * @return a client whose requests are relative to {@code baseURL}
     */
    @Bean
    public RestClient marketDataRestClient(
            @Value("${market-data.base-url}") String baseURL,
            @Value("${market-data.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${market-data.read-timeout-ms}") long readTimeoutMs){

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs)).build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseURL)
                .requestFactory(requestFactory)
                .build();

    }

}
