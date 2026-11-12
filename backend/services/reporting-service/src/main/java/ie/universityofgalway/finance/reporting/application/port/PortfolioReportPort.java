package ie.universityofgalway.finance.reporting.application.port;

import java.util.List;

import ie.universityofgalway.finance.reporting.domain.model.ReportInvestment;

public interface PortfolioReportPort {

    List<ReportInvestment> getPortfolioPositions(String accessToken);
}