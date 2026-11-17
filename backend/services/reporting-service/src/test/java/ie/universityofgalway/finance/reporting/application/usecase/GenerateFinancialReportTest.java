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
}