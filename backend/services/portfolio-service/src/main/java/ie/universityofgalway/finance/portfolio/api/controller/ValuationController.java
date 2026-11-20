package ie.universityofgalway.finance.portfolio.api.controller;

import ie.universityofgalway.finance.portfolio.api.dto.PortfolioValuationResponse;
import ie.universityofgalway.finance.portfolio.application.service.ValuationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes market valuations for portfolios owned by the authenticated user.
 */
@RestController
@RequestMapping("/api/portfolios")
public class ValuationController {
    private final ValuationService valuations;

    /**
     * Creates the valuation endpoint.
     *
     * @param valuations service that authorises ownership and values the holdings
     */
    public ValuationController(ValuationService valuations) {
        this.valuations = valuations;
    }

    /**
     * Returns holdings and valuation totals for the portfolio owned by the JWT subject.
     *
     * @param portfolioId portfolio to value
     * @param jwt authenticated token whose subject identifies the owner
     * @return holding valuations, including completeness and stale-price indicators
     * @throws ie.universityofgalway.finance.portfolio.domain.exception.PortfolioNotFoundException if the
     *     portfolio does not exist or belongs to another user
     */
    @GetMapping("/{portfolioId}/valuation")
    public PortfolioValuationResponse getValuation(@PathVariable Long portfolioId,
                                                  @AuthenticationPrincipal Jwt jwt) {
        return PortfolioValuationResponse.from(valuations.getValuation(portfolioId, jwt.getSubject()));
    }
}
