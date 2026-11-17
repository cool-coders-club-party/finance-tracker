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

    private final TransactionReportPort transactionReportPort;
    private final PortfolioReportPort portfolioReportPort;

    public GenerateFinancialReport(
            TransactionReportPort transactionReportPort,
            PortfolioReportPort portfolioReportPort) {
        this.transactionReportPort = transactionReportPort;
        this.portfolioReportPort = portfolioReportPort;
    }

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