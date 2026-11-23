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

/**
 * Retrieves current holdings from the Portfolio service for use in financial reports.
 */
@Component
public class PortfolioApiAdapter implements PortfolioReportPort {

    private final RestClient restClient;

    /**
     * Creates the adapter with the configured Portfolio service address.
     *
     * @param restClientBuilder builder for the HTTP client
     * @param baseUrl Portfolio service base URL
     */
    public PortfolioApiAdapter(RestClient.Builder restClientBuilder, @Value("${reporting.clients.portfolio-base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Lists the caller's portfolios and fetches each portfolio's holding performance.
     * The caller's bearer token is forwarded with every request.</p>
     *
     * @throws org.springframework.web.client.RestClientException if a Portfolio service request fails
     */
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

    /**
     * Portfolio entry returned by the Portfolio service.
     *
     * @param id portfolio identifier used to look up its holding performance
     * @param name portfolio display name
     */
    public record PortfolioResponse(Long id, String name) {
    }

    /**
     * Holding performance entry returned for a portfolio.
     *
     * @param ticker trading symbol of the holding
     * @param quantity number of units held
     * @param avgPurchasePrice average purchase price per unit
     * @param currentPrice current price per unit
     * @param costBasis amount invested in the remaining units
     * @param marketValue current market value of the holding
     * @param unrealizedPnl unrealised gain or loss on the open position
     * @param unrealizedPnlPercent unrealised gain or loss as a percentage of cost basis
     * @param realizedPnl realised gain or loss from closed trades
     */
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
