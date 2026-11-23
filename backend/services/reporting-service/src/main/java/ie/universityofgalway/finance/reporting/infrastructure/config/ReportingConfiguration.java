package ie.universityofgalway.finance.reporting.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ie.universityofgalway.finance.reporting.application.port.PortfolioReportPort;
import ie.universityofgalway.finance.reporting.application.port.TransactionReportPort;
import ie.universityofgalway.finance.reporting.application.usecase.GenerateFinancialReport;

/**
 * Wires the report generation use case with the service's data adapters.
 */
@Configuration
public class ReportingConfiguration {

    /**
     * Registers the use case that assembles financial reports.
     *
     * @param transactionReportPort Spring-managed transaction data source
     * @param portfolioReportPort Spring-managed portfolio data source
     * @return the report generator configured with both data sources
     */
    @Bean
    GenerateFinancialReport generateFinancialReport(
            TransactionReportPort transactionReportPort,
            PortfolioReportPort portfolioReportPort) {
        return new GenerateFinancialReport(
                transactionReportPort,
                portfolioReportPort);
    }
}
