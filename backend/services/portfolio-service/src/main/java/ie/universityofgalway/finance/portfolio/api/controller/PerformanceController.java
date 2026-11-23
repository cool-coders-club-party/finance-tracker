package ie.universityofgalway.finance.portfolio.api.controller;

import ie.universityofgalway.finance.portfolio.api.dto.HoldingPerformanceResponse;
import ie.universityofgalway.finance.portfolio.application.service.PerformanceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint for profit and loss per stock: {@code GET /api/portfolios/{id}/performance}.
 *
 * <p>The figures are rebuilt from trade history by {@link PerformanceService}. The caller's
 * user id always comes from their access token.</p>
 */
@RestController
@RequestMapping("/api/portfolios")
public class PerformanceController {

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    /**
     * Returns profit and loss for every stock ever traded in the portfolio, sorted by ticker.
     *
     * @param id  the portfolio id
     * @param jwt the caller's validated access token; its subject is the user id
     * @return one entry per stock, including closed positions (zero shares) with their realised profit
     * @throws ie.universityofgalway.finance.portfolio.domain.exception.PortfolioNotFoundException
     *         if the portfolio doesn't exist or isn't the caller's (404)
     */
    @GetMapping("/{id}/performance")
    public List<HoldingPerformanceResponse> performance(@PathVariable Long id,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return performanceService.getPerformance(id, jwt.getSubject()).stream()
                .map(HoldingPerformanceResponse::from)
                .toList();
    }
}