package ie.universityofgalway.finance.reporting.portfolio;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class PortfolioReportService {

    private final PortfolioClient portfolioClient;

    public PortfolioReportService(PortfolioClient portfolioClient) {
        this.portfolioClient = portfolioClient;
    }

    public List<PortfolioClient.HoldingPerformance> getInvestments(String jwtToken) {
        List<PortfolioClient.HoldingPerformance> investments = new ArrayList<>();

        List<PortfolioClient.Portfolio> portfolios =
                portfolioClient.getPortfolios(jwtToken);

        for (PortfolioClient.Portfolio portfolio : portfolios) {
            List<PortfolioClient.HoldingPerformance> holdings =
                    portfolioClient.getPerformance(portfolio.id(), jwtToken);

            for (PortfolioClient.HoldingPerformance holding : holdings) {
                if (holding.quantity().signum() > 0) {
                    investments.add(holding);
                }
            }
        }

        return List.copyOf(investments);
    }
}