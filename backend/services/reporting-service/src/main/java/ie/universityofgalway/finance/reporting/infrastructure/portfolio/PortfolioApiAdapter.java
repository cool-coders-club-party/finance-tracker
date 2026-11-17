package ie.universityofgalway.finance.reporting.infrastructure.portfolio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import ie.universityofgalway.finance.reporting.application.port.PortfolioReportPort;
import ie.universityofgalway.finance.reporting.domain.model.ReportInvestment;

@Component
public class PortfolioApiAdapter implements PortfolioReportPort {

    private final RestClient restClient;

    public PortfolioApiAdapter(RestClient.Builder restClientBuilder, @Value("${reporting.clients.portfolio-base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public List<ReportInvestment> getPortfolioPositions(String accessToken) {
        PortfolioResponse[] portfolios = restClient.get()
                .uri("/api/portfolios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(PortfolioResponse[].class);

        List<ReportInvestment> positions = new ArrayList<>();

        for (PortfolioResponse portfolio : portfolios) {
            HoldingPerformanceResponse[] holdings = restClient.get()
                    .uri("/api/portfolios/{id}/performance", portfolio.id())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(HoldingPerformanceResponse[].class);

            for (HoldingPerformanceResponse holding : holdings) {
                positions.add(new ReportInvestment(
                        holding.ticker(),
                        holding.quantity(),
                        holding.costBasis(),
                        holding.currentPrice()));
            }
        }

        return List.copyOf(positions);
    }

    public record PortfolioResponse(Long id, String name) {
    }

    public record HoldingPerformanceResponse(
            String ticker,
            BigDecimal quantity,
            BigDecimal avgPurchasePrice,
            BigDecimal currentPrice,
            BigDecimal costBasis,
            BigDecimal marketValue,
            BigDecimal unrealizedPnl,
            BigDecimal unrealizedPnlPercent,
            BigDecimal realizedPnl) {
    }
}