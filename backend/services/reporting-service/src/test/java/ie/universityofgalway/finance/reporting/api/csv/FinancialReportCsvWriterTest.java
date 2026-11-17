package ie.universityofgalway.finance.reporting.api.csv;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ie.universityofgalway.finance.reporting.domain.model.FinancialReport;
import ie.universityofgalway.finance.reporting.domain.model.ReportInvestment;
import ie.universityofgalway.finance.reporting.domain.model.ReportTransaction;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialReportCsvWriterTest {

    private final FinancialReportCsvWriter writer = new FinancialReportCsvWriter();

    @Test
    void writesHeadingTransactionAndInvestmentRows() {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        ReportTransaction transaction = new ReportTransaction(
                new BigDecimal("12.50"),
                "EUR",
                "Café",
                LocalDateTime.of(2026, 3, 14, 9, 30));

        ReportInvestment investment = new ReportInvestment(
                "AAPL",
                new BigDecimal("3"),
                new BigDecimal("450.00"),
                new BigDecimal("175.25"));

        FinancialReport report = new FinancialReport(
                userId, from, to, List.of(transaction), List.of(investment));

        String csv = new String(writer.write(report), StandardCharsets.UTF_8);

        assertThat(csv).contains(
                "User ID " + userId + " - Financial Summary from 2026-03-01 to 2026-03-31",
                "\"AMOUNT\",\"CURRENCY\",\"CATEGORY\",\"DATE & TIME\"",
                "\"12.50\",\"EUR\",\"Café\",\"2026-03-14T09:30:00\"",
                "\"AMOUNT INVESTED\",\"CURRENT PRICE\",\"TICKER\"",
                "\"450.00\",\"175.25\",\"AAPL\"");
    }

    @Test
    void escapesCommasAndQuotesInsideTransactionCategory() {
        ReportTransaction transaction = new ReportTransaction(
                new BigDecimal("3.25"),
                "EUR",
                "Food, \"Dining\"",
                LocalDateTime.of(2026, 3, 14, 9, 30));

        FinancialReport report = new FinancialReport(
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                List.of(transaction),
                List.of());

        String csv = new String(writer.write(report), StandardCharsets.UTF_8);

        assertThat(csv).contains(
                "\"3.25\",\"EUR\",\"Food, \"\"Dining\"\"\",\"2026-03-14T09:30:00\"\r\n");
    }

    @Test
    void includesSectionLabelsAndHeadersWhenThereAreNoRows() {
        FinancialReport report = new FinancialReport(
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                List.of(),
                List.of());

        String csv = new String(writer.write(report), StandardCharsets.UTF_8);

        assertThat(csv).contains(
                "\"TRANSACTIONS\",\"\",\"\",\"\"",
                "\"AMOUNT\",\"CURRENCY\",\"CATEGORY\",\"DATE & TIME\"",
                "\"INVESTMENTS\",\"\",\"\",\"\"",
                "\"AMOUNT INVESTED\",\"CURRENT PRICE\",\"TICKER\",\"\"");
    }
}