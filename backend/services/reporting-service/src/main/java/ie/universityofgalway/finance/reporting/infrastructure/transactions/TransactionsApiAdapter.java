package ie.universityofgalway.finance.reporting.infrastructure.transactions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import ie.universityofgalway.finance.reporting.application.port.TransactionReportPort;
import ie.universityofgalway.finance.reporting.domain.model.ReportTransaction;

@Component
public class TransactionsApiAdapter implements TransactionReportPort {

    private static final int PAGE_SIZE = 100;

    private final RestClient restClient;

    public TransactionsApiAdapter(RestClient.Builder restClientBuilder, @Value("${reporting.clients.transactions-base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public List<ReportTransaction> getTransactions(
            LocalDate from,
            LocalDate to,
            String accessToken) {

        List<ReportTransaction> transactions = new ArrayList<>();
        int page = 0;
        int totalPages;

        do {
            int currentPage = page;
            TransactionPageResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/transactions")
                            .queryParam("from", from)
                            .queryParam("to", to)
                            .queryParam("page", currentPage)
                            .queryParam("size", PAGE_SIZE)
                            .queryParam("sortBy", "date")
                            .queryParam("direction", "asc")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(TransactionPageResponse.class);

            for (TransactionResponse transaction : response.content()) {
                transactions.add(new ReportTransaction(
                        transaction.amount(),
                        transaction.currency(),
                        transaction.transactionCategory(),
                        transaction.occurredAt()));
            }

            totalPages = response.totalPages();
            page++;
        } while (page < totalPages);

        return List.copyOf(transactions);
    }

    public record TransactionPageResponse(
            List<TransactionResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    public record TransactionResponse(
            Long id,
            UUID userId,
            String transactionType,
            String transactionCategory,
            String currency,
            BigDecimal amount,
            String description,
            LocalDateTime occurredAt,
            LocalDateTime createdAt) {
    }
}