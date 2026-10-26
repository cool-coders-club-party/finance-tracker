package ie.universityofgalway.finance.portfolio.controller;

import ie.universityofgalway.finance.portfolio.dto.*;
import ie.universityofgalway.finance.portfolio.model.Portfolio;
import ie.universityofgalway.finance.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    // Lookup endpoint: lets a caller (e.g. the reporting service) discover which
    // portfolio ids belong to the current user before requesting holdings.
    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getPortfolios() {
        String userId = "user-1"; // placeholder until real auth identifies the caller

        var portfolios = portfolioService.getPortfolios(userId).stream()
                .map(PortfolioResponse::from)
                .toList();

        return ResponseEntity.ok(portfolios);
    }

    @GetMapping("/{portfolioId}/holdings")
    public ResponseEntity<List<HoldingResponse>> getHoldings(@PathVariable Long portfolioId) {
        String userId = "user-1"; // same placeholder as buy()/sell(), see earlier note

        var holdings = portfolioService.getHoldings(portfolioId, userId).stream()
                .map(HoldingResponse::from)
                .toList();

        return ResponseEntity.ok(holdings);
    }

    @GetMapping("/{portfolioId}/trades")
    public ResponseEntity<List<TradeResponse>> getTrades(@PathVariable Long portfolioId) {
        String userId = "user-1";

        var trades = portfolioService.getTrades(portfolioId, userId).stream()
                .map(TradeResponse::from).toList();

        return ResponseEntity.ok(trades);
    }

    @PostMapping("/{portfolioId}/buy")
    public ResponseEntity<HoldingResponse> buy(@PathVariable Long portfolioId,
                                                @RequestBody @Valid TradeRequest request) {
        // TODO: userId should come from the authenticated principal once auth is
        // wired up, not be hardcoded - see note below.
        String userId = "user-1";

        var holding = portfolioService.buy(portfolioId, userId, request.ticker(), request.quantity());
        return ResponseEntity.ok(HoldingResponse.from(holding));
    }

    @PostMapping("/{portfolioId}/sell")
    public ResponseEntity<HoldingResponse> sell(@PathVariable Long portfolioId,
                                                 @RequestBody @Valid TradeRequest request) {
        String userId = "user-1";

        var holding = portfolioService.sell(portfolioId, userId, request.ticker(), request.quantity());
        return ResponseEntity.ok(HoldingResponse.from(holding));
    }

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(@RequestBody @Valid CreatePortfolioRequest request) {
        String userId = "user-1";

        Portfolio created = portfolioService.createPortfolio(userId, request.name());

        URI location = URI.create("/api/portfolios/" + created.getId());
        return ResponseEntity.created(location).body(PortfolioResponse.from(created));
    }
}
