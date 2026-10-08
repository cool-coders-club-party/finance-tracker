package ie.universityofgalway.finance.portfolio.controller;

import ie.universityofgalway.finance.portfolio.dto.HoldingResponse;
import ie.universityofgalway.finance.portfolio.dto.TradeRequest;
import ie.universityofgalway.finance.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/holdings")
    public ResponseEntity<List<HoldingResponse>> getHoldings(@PathVariable Long portfolioId) {
        String userId = "user-1"; // same placeholder as buy()/sell(), see earlier note

        var holdings = portfolioService.getHoldings(portfolioId, userId).stream()
                .map(HoldingResponse::from)
                .toList();

        return ResponseEntity.ok(holdings);
    }

    @PostMapping("/buy")
    public ResponseEntity<HoldingResponse> buy(@PathVariable Long portfolioId,
                                                @RequestBody @Valid TradeRequest request) {
        // TODO: userId should come from the authenticated principal once auth is
        // wired up, not be hardcoded - see note below.
        String userId = "user-1";

        var holding = portfolioService.buy(portfolioId, userId, request.ticker(), request.quantity());
        return ResponseEntity.ok(HoldingResponse.from(holding));
    }

    @PostMapping("/sell")
    public ResponseEntity<HoldingResponse> sell(@PathVariable Long portfolioId,
                                                 @RequestBody @Valid TradeRequest request) {
        String userId = "user-1";

        var holding = portfolioService.sell(portfolioId, userId, request.ticker(), request.quantity());
        return ResponseEntity.ok(HoldingResponse.from(holding));
    }
}
