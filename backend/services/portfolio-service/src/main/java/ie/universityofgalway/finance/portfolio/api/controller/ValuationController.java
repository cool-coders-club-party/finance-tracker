package ie.universityofgalway.finance.portfolio.api.controller;

import ie.universityofgalway.finance.portfolio.api.dto.PortfolioValuationResponse;
import ie.universityofgalway.finance.portfolio.application.service.ValuationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolios")
public class ValuationController {
    private final ValuationService valuations;

    public ValuationController(ValuationService valuations) {
        this.valuations = valuations;
    }

    @GetMapping("/{portfolioId}/valuation")
    public PortfolioValuationResponse getValuation(@PathVariable Long portfolioId,
                                                  @AuthenticationPrincipal Jwt jwt) {
        return PortfolioValuationResponse.from(valuations.getValuation(portfolioId, jwt.getSubject()));
    }
}
