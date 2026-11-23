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

/**
 * Retrieves paginated transaction history from the Transactions service.
 */
@Component
public class TransactionsApiAdapter implements TransactionReportPort {

    private static final int PAGE_SIZE = 100;

    private final RestClient restClient;

    /**
     * Creates the adapter with the configured Transactions service address.
     *
     * @param restClientBuilder builder for the HTTP client
     * @param baseUrl Transactions service base URL
     */
    public TransactionsApiAdapter(RestClient.Builder restClientBuilder, @Value("${reporting.clients.transactions-base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Requests pages from the Transactions service starting at page zero and
     * uses the reported page count to retrieve all results. Each request forwards
     * the caller's bearer token and asks for ascending date order.</p>
     *
     * @throws org.springframework.web.client.RestClientException if a Transactions service request fails
     */
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

    /**
     * Page of transaction history returned by the Transactions service.
     *
     * @param content transactions on the current page
     * @param page zero-based index of the current page
     * @param size page size used by the service
     * @param totalElements number of matching transactions across all pages
     * @param totalPages number of pages in the result
     */
    public record TransactionPageResponse(
            List<TransactionResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    /**
     * Transaction entry returned in the history response.
     *
     * @param id transaction identifier
     * @param userId ID of the transaction owner
     * @param transactionType type assigned to the transaction
     * @param transactionCategory category assigned to the transaction
     * @param currency transaction currency code
     * @param amount transaction amount in its original currency
     * @param description description supplied with the transaction
     * @param occurredAt local date and time when the transaction occurred
     * @param createdAt local date and time when the record was created
     */
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
