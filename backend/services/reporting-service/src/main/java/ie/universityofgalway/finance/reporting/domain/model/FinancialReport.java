package ie.universityofgalway.finance.reporting.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FinancialReport(
        UUID userId,
        LocalDate from,
        LocalDate to,
        List<ReportTransaction> transactions,
        List<ReportInvestment> investments) {

    public FinancialReport {
        transactions = List.copyOf(transactions);
        investments = List.copyOf(investments);
    }
}