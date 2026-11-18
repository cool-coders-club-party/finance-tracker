package ie.universityofgalway.finance.reporting.infrastructure.transactions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import ie.universityofgalway.finance.reporting.domain.model.ReportTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TransactionsApiAdapterTest {

    private MockRestServiceServer server;
    private TransactionsApiAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        adapter = new TransactionsApiAdapter(
                builder, "https://transactions.test");
    }

    @Test
    void sendsBearerTokenAndMapsTransactionResponse() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        String responseBody = """
                {
                  "content": [{
                    "id": 1,
                    "userId": "123e4567-e89b-12d3-a456-426614174000",
                    "transactionType": "EXPENSE",
                    "transactionCategory": "GROCERIES",
                    "currency": "EUR",
                    "amount": 12.50,
                    "description": "Groceries",
                    "occurredAt": "2026-03-14T09:30:00",
                    "createdAt": "2026-03-14T09:31:00"
                  }],
                  "page": 0,
                  "size": 100,
                  "totalElements": 1,
                  "totalPages": 1
                }
                """;

        server.expect(requestTo(
                        "https://transactions.test/api/v1/transactions"
                                + "?from=2026-03-01&to=2026-03-31"
                                + "&page=0&size=100&sortBy=date&direction=asc"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess(
                        responseBody, MediaType.APPLICATION_JSON));

        var result = adapter.getTransactions(from, to, "test-token");

        assertThat(result).containsExactly(new ReportTransaction(
                new BigDecimal("12.50"),
                "EUR",
                "GROCERIES",
                LocalDateTime.of(2026, 3, 14, 9, 30)));

        server.verify();
    }

    @Test
    void fetchesTransactionsFromEveryPage() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        String firstPage = """
            {
              "content": [{
                "transactionCategory": "GROCERIES",
                "currency": "EUR",
                "amount": 12.50,
                "occurredAt": "2026-03-14T09:30:00"
              }],
              "page": 0,
              "size": 100,
              "totalElements": 2,
              "totalPages": 2
            }
            """;

        String secondPage = """
            {
              "content": [{
                "transactionCategory": "TRANSPORT",
                "currency": "EUR",
                "amount": 3.25,
                "occurredAt": "2026-03-15T10:00:00"
              }],
              "page": 1,
              "size": 100,
              "totalElements": 2,
              "totalPages": 2
            }
            """;

        server.expect(requestTo(
                        "https://transactions.test/api/v1/transactions"
                                + "?from=2026-03-01&to=2026-03-31"
                                + "&page=0&size=100&sortBy=date&direction=asc"))
                .andRespond(withSuccess(firstPage, MediaType.APPLICATION_JSON));

        server.expect(requestTo(
                        "https://transactions.test/api/v1/transactions"
                                + "?from=2026-03-01&to=2026-03-31"
                                + "&page=1&size=100&sortBy=date&direction=asc"))
                .andRespond(withSuccess(secondPage, MediaType.APPLICATION_JSON));

        var result = adapter.getTransactions(from, to, "test-token");

        assertThat(result).containsExactly(
                new ReportTransaction(
                        new BigDecimal("12.50"),
                        "EUR",
                        "GROCERIES",
                        LocalDateTime.of(2026, 3, 14, 9, 30)),
                new ReportTransaction(
                        new BigDecimal("3.25"),
                        "EUR",
                        "TRANSPORT",
                        LocalDateTime.of(2026, 3, 15, 10, 0)));

        server.verify();
    }
}