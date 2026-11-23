package ie.universityofgalway.finance.reporting.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Financial report data for one user and transaction date range.
 *
 * @param userId ID of the user named in the report
 * @param from first transaction date in scope
 * @param to last transaction date in scope
 * @param transactions unmodifiable list of report transactions
 * @param investments unmodifiable list of current portfolio holdings
 */
public record FinancialReport(
        UUID userId,
        LocalDate from,
        LocalDate to,
        List<ReportTransaction> transactions,
        List<ReportInvestment> investments) {

    /**
     * Creates a report with unmodifiable copies of the supplied lists.
     *
     * @param userId ID of the user named in the report
     * @param from first transaction date in scope
     * @param to last transaction date in scope
     * @param transactions report transactions to copy
     * @param investments current portfolio holdings to copy
     * @throws NullPointerException if either list or one of its elements is null
     */
    public FinancialReport {
        transactions = List.copyOf(transactions);
        investments = List.copyOf(investments);
    }
}
