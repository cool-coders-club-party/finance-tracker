package ie.universityofgalway.finance.marketdata.api;

import ie.universityofgalway.finance.marketdata.application.PriceHistoryService;
import ie.universityofgalway.finance.marketdata.domain.MarketDataUnavailableException;
import ie.universityofgalway.finance.marketdata.domain.PricePoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/quotes")
public class PriceHistoryController {
    private final PriceHistoryService historyService;

    public PriceHistoryController(PriceHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/{symbol}/history")
    public ResponseEntity<Object> getHistory(@PathVariable String symbol) {
        Optional<List<PricePoint>> result = historyService.getHistory(symbol);

        // No history exists for this ticker.
        if (result.isEmpty()) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "No price history found for symbol: " + symbol
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
        }

        // Send simple date-and-price points to the frontend.
        return ResponseEntity.ok(PriceHistoryResponse.from(result.get()));
    }

    @ExceptionHandler(MarketDataUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleUnavailable(
            MarketDataUnavailableException exception
    ) {
        // Keep provider failures distinct from unknown tickers.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}