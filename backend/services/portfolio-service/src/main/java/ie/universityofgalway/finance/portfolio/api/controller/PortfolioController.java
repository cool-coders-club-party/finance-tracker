package ie.universityofgalway.finance.portfolio.api.controller;

import ie.universityofgalway.finance.portfolio.api.dto.*;
import ie.universityofgalway.finance.portfolio.application.service.PortfolioService;
import ie.universityofgalway.finance.portfolio.domain.model.Portfolio;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST controller for portfolios, holdings, trades, buying and selling.
 *
 * <p>All endpoints are under {@code /api/portfolios} and require a valid access token:
 * <ul>
 *   <li>{@code GET /api/portfolios} - list the caller's portfolios</li>
 *   <li>{@code POST /api/portfolios} - create a portfolio</li>
 *   <li>{@code GET /api/portfolios/{id}/holdings} - holdings, without prices</li>
 *   <li>{@code GET /api/portfolios/{id}/trades} - every buy and sell, oldest first</li>
 *   <li>{@code POST /api/portfolios/{id}/buy} - buy at market price, or record an earlier purchase</li>
 *   <li>{@code POST /api/portfolios/{id}/sell} - sell at market price</li>
 * </ul>
 * Valuation and performance have their own controllers.
 *
 * <p>This class only translates between HTTP and Java. The user id is always taken from the
 * token's subject, never from the request, and business rules live in {@link PortfolioService}.
 * Errors are turned into responses by {@link GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    /**
     * Lists the caller's portfolios. Lets a caller (the frontend, or the reporting service)
     * discover which portfolio ids belong to the user before requesting holdings.
     *
     * @param jwt the caller's validated access token
     * @return {@code 200 OK} with the portfolios, or an empty list if the user has none
     */
    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getPortfolios(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        var portfolios = portfolioService.getPortfolios(userId).stream()
                .map(PortfolioResponse::from)
                .toList();

        return ResponseEntity.ok(portfolios);
    }

    /**
     * Lists the holdings in one of the caller's portfolios, without market prices.
     * Holdings sold down to zero shares are included.
     *
     * @param portfolioId the portfolio id
     * @param jwt         the caller's validated access token
     * @return {@code 200 OK} with the holdings
     * @throws ie.universityofgalway.finance.portfolio.domain.exception.PortfolioNotFoundException
     *         if the portfolio doesn't exist or isn't the caller's (404)
     */
    @GetMapping("/{portfolioId}/holdings")
    public ResponseEntity<List<HoldingResponse>> getHoldings(@PathVariable Long portfolioId,
                                                             @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        var holdings = portfolioService.getHoldings(portfolioId, userId).stream()
                .map(HoldingResponse::from)
                .toList();

        return ResponseEntity.ok(holdings);
    }

    /**
     * Lists every buy and sell in one of the caller's portfolios, oldest first.
     * A backdated purchase appears at the date it happened, not when it was entered.
     *
     * @param portfolioId the portfolio id
     * @param jwt         the caller's validated access token
     * @return {@code 200 OK} with the trades
     * @throws ie.universityofgalway.finance.portfolio.domain.exception.PortfolioNotFoundException
     *         if the portfolio doesn't exist or isn't the caller's (404)
     */
    @GetMapping("/{portfolioId}/trades")
    public ResponseEntity<List<TradeResponse>> getTrades(@PathVariable Long portfolioId,
                                                         @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        var trades = portfolioService.getTrades(portfolioId, userId).stream()
                .map(TradeResponse::from)
                .toList();

        return ResponseEntity.ok(trades);
    }

    /**
     * Buys shares at the current market price, or records a purchase made earlier when the
     * request includes both {@code price} and {@code executedAt}.
     *
     * @param portfolioId the portfolio receiving the shares
     * @param request     ticker and quantity, plus optional price and time for an earlier purchase
     * @param jwt         the caller's validated access token
     * @return {@code 200 OK} with the updated holding
     * @see PortfolioService#buy(Long, String, String, java.math.BigDecimal, java.math.BigDecimal, java.time.Instant)
     */
    @PostMapping("/{portfolioId}/buy")
    public ResponseEntity<HoldingResponse> buy(@PathVariable Long portfolioId,
                                               @RequestBody @Valid TradeRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        var holding = portfolioService.buy(portfolioId, userId, request.ticker(), request.quantity(),
                request.price(), request.executedAt());
        return ResponseEntity.ok(HoldingResponse.from(holding));
    }

    /**
     * Sells shares at the current market price. Backdated sales aren't supported, so a request
     * that includes {@code price} or {@code executedAt} is rejected rather than silently ignored.
     *
     * @param portfolioId the portfolio holding the shares
     * @param request     ticker and quantity; {@code price} and {@code executedAt} must be absent
     * @param jwt         the caller's validated access token
     * @return {@code 200 OK} with the updated holding (its quantity may now be zero)
     * @throws IllegalArgumentException if {@code price} or {@code executedAt} is given (400)
     * @see PortfolioService#sell(Long, String, String, java.math.BigDecimal)
     */
    @PostMapping("/{portfolioId}/sell")
    public ResponseEntity<HoldingResponse> sell(@PathVariable Long portfolioId,
                                                @RequestBody @Valid TradeRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        // Backdating is only supported for buys; don't silently ignore the fields.
        if (request.price() != null || request.executedAt() != null) {
            throw new IllegalArgumentException("Recording an earlier sale is not supported yet");
        }
        var holding = portfolioService.sell(portfolioId, userId, request.ticker(), request.quantity());
        return ResponseEntity.ok(HoldingResponse.from(holding));
    }

    /**
     * Creates a portfolio owned by the caller.
     *
     * @param request the validated portfolio name
     * @param jwt     the caller's validated access token
     * @return {@code 201 Created} with a {@code Location} header and the new portfolio
     */
    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(@RequestBody @Valid CreatePortfolioRequest request,
                                                             @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        Portfolio created = portfolioService.createPortfolio(userId, request.name());

        URI location = URI.create("/api/portfolios/" + created.getId());
        return ResponseEntity.created(location).body(PortfolioResponse.from(created));
    }
}