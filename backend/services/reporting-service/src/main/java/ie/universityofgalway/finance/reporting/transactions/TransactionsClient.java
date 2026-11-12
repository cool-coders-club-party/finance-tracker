package ie.universityofgalway.finance.reporting.transactions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TransactionsClient {

    private final RestClient restClient;

    public TransactionsClient(
            @Value("${reporting.clients.transactions-base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public TransactionPage getPage(
            LocalDate from,
            LocalDate to,
            int page,
            String jwtToken) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .queryParam("page", page)
                        .queryParam("size", 100)
                        .queryParam("sortBy", "date")
                        .queryParam("direction", "asc")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .retrieve()
                .body(TransactionPage.class);
    }

    public record TransactionPage(
            List<Transaction> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    public record Transaction(
            BigDecimal amount,
            String transactionCategory,
            String currency,
            LocalDateTime occurredAt) {
    }
}