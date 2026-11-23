package ie.universityofgalway.finance.reporting.application.port;

import java.util.List;

import ie.universityofgalway.finance.reporting.domain.model.ReportInvestment;

/**
 * Supplies current portfolio holdings for financial reports.
 */
public interface PortfolioReportPort {

    /**
     * Retrieves the caller's current holdings across their portfolios.
     *
     * @param accessToken caller's bearer token value without the {@code Bearer} prefix
     * @return one report investment per holding, or an empty list if there are none
     */
    List<ReportInvestment> getPortfolioPositions(String accessToken);
}
