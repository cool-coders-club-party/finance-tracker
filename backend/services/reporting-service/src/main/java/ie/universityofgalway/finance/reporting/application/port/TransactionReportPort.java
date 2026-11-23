package ie.universityofgalway.finance.reporting.application.port;

import java.time.LocalDate;
import java.util.List;

import ie.universityofgalway.finance.reporting.domain.model.ReportTransaction;

/**
 * Supplies transaction history for financial reports.
 */
public interface TransactionReportPort {

    /**
     * Retrieves the caller's transactions within an inclusive date range.
     *
     * @param from first transaction date to include
     * @param to last transaction date to include
     * @param accessToken caller's bearer token value without the {@code Bearer} prefix
     * @return matching report transactions, or an empty list if there are none
     */
    List<ReportTransaction> getTransactions(
            LocalDate from,
            LocalDate to,
            String accessToken);
}
