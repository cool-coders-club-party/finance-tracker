package ie.universityofgalway.finance.reporting.application.usecase;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import ie.universityofgalway.finance.reporting.application.port.PortfolioReportPort;
import ie.universityofgalway.finance.reporting.application.port.TransactionReportPort;
import ie.universityofgalway.finance.reporting.domain.model.FinancialReport;
import ie.universityofgalway.finance.reporting.domain.model.ReportInvestment;
import ie.universityofgalway.finance.reporting.domain.model.ReportTransaction;

/**
 * Assembles a financial report from transaction history and current portfolio holdings.
 */
public final class GenerateFinancialReport {

    private final TransactionReportPort transactionReportPort;
    private final PortfolioReportPort portfolioReportPort;

    /**
     * Creates the use case with its transaction and portfolio data sources.
     *
     * @param transactionReportPort source of transactions within the requested date range
     * @param portfolioReportPort source of current portfolio holdings
     */
    public GenerateFinancialReport(
            TransactionReportPort transactionReportPort,
            PortfolioReportPort portfolioReportPort) {
        this.transactionReportPort = transactionReportPort;
        this.portfolioReportPort = portfolioReportPort;
    }

    /**
     * Builds a report for the requested transaction date range.
     *
     * <p>Transactions are sorted by occurrence time, oldest first. Current holdings
     * with a positive quantity are included regardless of the transaction date range.</p>
     *
     * @param userId user ID recorded in the report
     * @param from first transaction date to include
     * @param to last transaction date to include
     * @param accessToken caller's bearer token value passed to both data sources
     * @return the assembled report with its transaction and investment data
     * @throws IllegalArgumentException if {@code from} is after {@code to}
     */
    public FinancialReport generate(
            UUID userId,
            LocalDate from,
            LocalDate to,
            String accessToken) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be on or before to");
        }

        List<ReportTransaction> transactions =
                transactionReportPort.getTransactions(from, to, accessToken).stream()
                        .sorted(Comparator.comparing(ReportTransaction::occurredAt))
                        .toList();

        List<ReportInvestment> investments =
                portfolioReportPort.getPortfolioPositions(accessToken).stream()
                        .filter(investment -> investment.quantity().signum() > 0)
                        .toList();

        return new FinancialReport(userId, from, to, transactions, investments);
    }
}
