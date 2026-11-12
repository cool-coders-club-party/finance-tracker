package ie.universityofgalway.finance.reporting.portfolio;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PortfolioClient {

    private final RestClient restClient;

    public PortfolioClient(
            @Value("${reporting.clients.portfolio-base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<Portfolio> getPortfolios(String jwtToken) {
        Portfolio[] portfolios = restClient.get()
                .uri("/api/portfolios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .retrieve()
                .body(Portfolio[].class);

        return List.of(portfolios);
    }

    public List<HoldingPerformance> getPerformance(Long portfolioId, String jwtToken) {
        HoldingPerformance[] holdings = restClient.get()
                .uri("/api/portfolios/{id}/performance", portfolioId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .retrieve()
                .body(HoldingPerformance[].class);

        return List.of(holdings);
    }

    public record Portfolio(Long id, String name) {
    }

    public record HoldingPerformance(
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