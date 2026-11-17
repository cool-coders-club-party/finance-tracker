package ie.universityofgalway.finance.reporting.api;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;

import ie.universityofgalway.finance.reporting.api.csv.FinancialReportCsvWriter;
import ie.universityofgalway.finance.reporting.application.port.PortfolioReportPort;
import ie.universityofgalway.finance.reporting.application.port.TransactionReportPort;
import ie.universityofgalway.finance.reporting.application.usecase.GenerateFinancialReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.verifyNoInteractions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialReportControllerTest {

    private TransactionReportPort transactionReportPort;
    private PortfolioReportPort portfolioReportPort;
    private FinancialReportController controller;

    @BeforeEach
    void setUp() {
        transactionReportPort = mock(TransactionReportPort.class);
        portfolioReportPort = mock(PortfolioReportPort.class);

        GenerateFinancialReport useCase = new GenerateFinancialReport(
                transactionReportPort, portfolioReportPort);

        controller = new FinancialReportController(
                useCase, new FinancialReportCsvWriter());
    }

    @Test
    void returnsCsvAttachmentForJwtSubject() {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String accessToken = "test-token";
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        when(transactionReportPort.getTransactions(from, to, accessToken))
                .thenReturn(List.of());
        when(portfolioReportPort.getPortfolioPositions(accessToken))
                .thenReturn(List.of());

        Jwt jwt = Jwt.withTokenValue(accessToken)
                .header("alg", "HS256")
                .subject(userId.toString())
                .build();

        var response = controller.export(jwt, from, to);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(new MediaType("text", "csv", StandardCharsets.UTF_8));
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment", "financial-summary-2026-03-01-to-2026-03-31.csv");

        String csv = new String(response.getBody(), StandardCharsets.UTF_8);
        assertThat(csv).contains(
                "User ID " + userId + " - Financial Summary from 2026-03-01 to 2026-03-31");

        verify(transactionReportPort).getTransactions(from, to, accessToken);
        verify(portfolioReportPort).getPortfolioPositions(accessToken);
    }

    @Test
    void rejectsInvalidDateRangeBeforeCallingReportPorts() {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .build();

        LocalDate from = LocalDate.of(2026, 3, 2);
        LocalDate to = LocalDate.of(2026, 3, 1);

        assertThatThrownBy(() -> controller.export(jwt, from, to))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason())
                            .isEqualTo("from must be on or before to");
                });

        verifyNoInteractions(transactionReportPort, portfolioReportPort);
    }
}