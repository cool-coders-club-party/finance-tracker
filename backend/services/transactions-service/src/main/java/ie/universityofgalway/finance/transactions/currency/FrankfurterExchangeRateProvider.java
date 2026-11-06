package ie.universityofgalway.finance.transactions.currency;

import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;
import ie.universityofgalway.finance.transactions.exception.ExchangeRateUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;

import static ie.universityofgalway.finance.transactions.util.validator.ExchangeRateFetchResponseValidator.validateExchangeRateResponse;

@Component
public class FrankfurterExchangeRateProvider implements ExchangeRateProvider {

    private final RestClient restClient;

    public FrankfurterExchangeRateProvider(
            RestClient.Builder restClientBuilder,
            @Value("${frankfurter.base-url}") String baseUrl,
            @Value("${frankfurter.connect-timeout}") Duration connectTimeout,
            @Value("${frankfurter.read-timeout}") Duration readTimeout
    ) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(readTimeout);

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public ExchangeRate getRate(
            TransactionCurrency baseCurrency,
            TransactionCurrency targetCurrency,
            LocalDate date
    ) {
        Objects.requireNonNull(baseCurrency);
        Objects.requireNonNull(targetCurrency);
        Objects.requireNonNull(date);

        if (baseCurrency == targetCurrency) {
            return new ExchangeRate(
                    baseCurrency,
                    targetCurrency,
                    date,
                    BigDecimal.ONE
            );
        }

        FrankfurterRateResponse response;
        try {
            response = fetchRate(baseCurrency, targetCurrency, date);
        } catch (RestClientException exception) {
            throw new ExchangeRateUnavailableException(
                    "Could not retrieve the exchange rate",
                    exception
            );
        }

        validateExchangeRateResponse(baseCurrency, targetCurrency, date, response);

        return new ExchangeRate(
                baseCurrency,
                targetCurrency,
                response.date(),
                response.rate()
        );
    }

    private FrankfurterRateResponse fetchRate(TransactionCurrency baseCurrency, TransactionCurrency targetCurrency, LocalDate date) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/providers/ecb/rate/{base}/{quote}")
                        .queryParam("date", date)
                        .build(
                                baseCurrency.name(),
                                targetCurrency.name()
                        ))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(FrankfurterRateResponse.class);
    }


}
