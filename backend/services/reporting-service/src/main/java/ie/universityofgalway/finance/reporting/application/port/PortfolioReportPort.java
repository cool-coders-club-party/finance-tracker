package ie.universityofgalway.finance.reporting.application.port;

import java.util.List;

import ie.universityofgalway.finance.reporting.domain.model.ReportInvestment;

public interface PortfolioReportPort {
    // Reporting use case requires a list of investments.
    // A portfolio position is one holding in someone's portfolio.
    List<ReportInvestment> getPortfolioPositions(String accessToken);
}