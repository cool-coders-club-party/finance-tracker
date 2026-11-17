package ie.universityofgalway.finance.reporting.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ie.universityofgalway.finance.reporting.application.port.PortfolioReportPort;
import ie.universityofgalway.finance.reporting.application.port.TransactionReportPort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import ie.universityofgalway.finance.reporting.domain.model.ReportTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class GenerateFinancialReportTest {

    private TransactionReportPort transactionReportPort;
    private PortfolioReportPort portfolioReportPort;
    private GenerateFinancialReport useCase;

    @BeforeEach
    void setUp() {
        transactionReportPort = mock(TransactionReportPort.class);
        portfolioReportPort = mock(PortfolioReportPort.class);
        useCase = new GenerateFinancialReport(transactionReportPort, portfolioReportPort);
    }

    @Test
    void rejectsDateRangeWhenFromIsAfterTo() {
        LocalDate from = LocalDate.of(2026, 3, 2);
        LocalDate to = LocalDate.of(2026, 3, 1);

        assertThatThrownBy(() ->
                useCase.generate(UUID.randomUUID(), from, to, "test-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("from must be on or before to");

        verifyNoInteractions(transactionReportPort, portfolioReportPort);
    }

    @Test
    void sortsTransactionsOldestFirst() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        ReportTransaction newer = new ReportTransaction(
                new BigDecimal("20.00"),
                "EUR",
                "FOOD",
                LocalDateTime.of(2026, 3, 14, 12, 0));

        ReportTransaction older = new ReportTransaction(
                new BigDecimal("10.00"),
                "EUR",
                "TRANSPORT",
                LocalDateTime.of(2026, 3, 14, 9, 30));

        when(transactionReportPort.getTransactions(from, to, "test-token"))
                .thenReturn(List.of(newer, older));
        when(portfolioReportPort.getPortfolioPositions("test-token"))
                .thenReturn(List.of());

        var report = useCase.generate(UUID.randomUUID(), from, to, "test-token");

        assertThat(report.transactions()).containsExactly(older, newer);
    }
}