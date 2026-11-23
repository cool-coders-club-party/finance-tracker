package ie.universityofgalway.finance.reporting.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FinancialReport(
        // Declare data we need for financial report.
        UUID userId,
        LocalDate from,
        LocalDate to,
        List<ReportTransaction> transactions,
        List<ReportInvestment> investments) {

    public FinancialReport {
        // Create a safe, unmodifiable copy of both lists.
        // This prevents a report's contents from being changes through those lists after the report is created.
        transactions = List.copyOf(transactions);
        investments = List.copyOf(investments);
    }
}