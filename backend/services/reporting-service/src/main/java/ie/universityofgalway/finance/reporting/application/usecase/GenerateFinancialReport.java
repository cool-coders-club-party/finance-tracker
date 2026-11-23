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

public final class GenerateFinancialReport {

    // Financial report keeps references to the transaction/portfolio ports.
    private final TransactionReportPort transactionReportPort;
    private final PortfolioReportPort portfolioReportPort;

    public GenerateFinancialReport(
            TransactionReportPort transactionReportPort,
            PortfolioReportPort portfolioReportPort) {
        this.transactionReportPort = transactionReportPort;
        this.portfolioReportPort = portfolioReportPort;
    }
    // Recevies the user ID, report date range, and access token.
    public FinancialReport generate(
            UUID userId,
            LocalDate from,
            LocalDate to,
            String accessToken) {
        // Reject where from is after to.
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be on or before to");
        }
        // Get a list of transactions, sort them by date.
        List<ReportTransaction> transactions =
                transactionReportPort.getTransactions(from, to, accessToken).stream()
                        .sorted(Comparator.comparing(ReportTransaction::occurredAt))
                        .toList();
        // Do the same with portfolio.
        List<ReportInvestment> investments =
                portfolioReportPort.getPortfolioPositions(accessToken).stream()
                        .filter(investment -> investment.quantity().signum() > 0)
                        .toList();
        // Return a put-together financial report.
        return new FinancialReport(userId, from, to, transactions, investments);
    }
}