package ie.universityofgalway.finance.reporting.infrastructure.portfolio;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import ie.universityofgalway.finance.reporting.domain.model.ReportInvestment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PortfolioApiAdapterTest {

    private MockRestServiceServer server;
    private PortfolioApiAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        adapter = new PortfolioApiAdapter(builder, "https://portfolio.test");
    }

    @Test
    void mapsPortfolioPerformanceAndSendsBearerToken() {
        server.expect(requestTo("https://portfolio.test/api/portfolios"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess("""
                        [{"id":7,"name":"Long-term"}]
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(
                        "https://portfolio.test/api/portfolios/7/performance"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess("""
                        [{
                          "ticker": "AAPL",
                          "quantity": 3,
                          "avgPurchasePrice": 150.00,
                          "currentPrice": 175.25,
                          "costBasis": 450.00,
                          "marketValue": 525.75,
                          "unrealizedPnl": 75.75,
                          "unrealizedPnlPercent": 16.83,
                          "realizedPnl": 0.00
                        }]
                        """, MediaType.APPLICATION_JSON));

        var result = adapter.getPortfolioPositions("test-token");

        assertThat(result).containsExactly(new ReportInvestment(
                "AAPL",
                new BigDecimal("3"),
                new BigDecimal("450.00"),
                new BigDecimal("175.25")));

        server.verify();
    }

    @Test
    void combinesHoldingsFromEveryPortfolio() {
        server.expect(requestTo("https://portfolio.test/api/portfolios"))
                .andRespond(withSuccess("""
                    [
                      {"id":7,"name":"Long-term"},
                      {"id":8,"name":"Retirement"}
                    ]
                    """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(
                        "https://portfolio.test/api/portfolios/7/performance"))
                .andRespond(withSuccess("""
                    [{
                      "ticker": "AAPL",
                      "quantity": 2,
                      "costBasis": 200.00,
                      "currentPrice": 150.00
                    }]
                    """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(
                        "https://portfolio.test/api/portfolios/8/performance"))
                .andRespond(withSuccess("""
                    [{
                      "ticker": "MSFT",
                      "quantity": 1,
                      "costBasis": 300.00,
                      "currentPrice": 350.00
                    }]
                    """, MediaType.APPLICATION_JSON));

        var result = adapter.getPortfolioPositions("test-token");

        assertThat(result).containsExactly(
                new ReportInvestment(
                        "AAPL",
                        new BigDecimal("2"),
                        new BigDecimal("200.00"),
                        new BigDecimal("150.00")),
                new ReportInvestment(
                        "MSFT",
                        new BigDecimal("1"),
                        new BigDecimal("300.00"),
                        new BigDecimal("350.00")));

        server.verify();
    }
}