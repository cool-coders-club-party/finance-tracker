package ie.universityofgalway.finance.portfolio.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "price-client.type", havingValue = "market-data")
public class MarketDataClient {
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
