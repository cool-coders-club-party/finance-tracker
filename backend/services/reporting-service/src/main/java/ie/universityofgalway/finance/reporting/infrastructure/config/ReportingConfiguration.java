package ie.universityofgalway.finance.reporting.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ie.universityofgalway.finance.reporting.application.port.PortfolioReportPort;
import ie.universityofgalway.finance.reporting.application.port.TransactionReportPort;
import ie.universityofgalway.finance.reporting.application.usecase.GenerateFinancialReport;

@Configuration
public class ReportingConfiguration {
    // Receives transaction and portfolio ports. Spring supplies their implementation (API adapters).
    @Bean
    GenerateFinancialReport generateFinancialReport(
            TransactionReportPort transactionReportPort,
            PortfolioReportPort portfolioReportPort) {
        return new GenerateFinancialReport(
                transactionReportPort,
                portfolioReportPort);
    }
}