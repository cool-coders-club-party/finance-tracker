package ie.universityofgalway.finance.marketdata.api;

import ie.universityofgalway.finance.marketdata.application.QuoteService;
import ie.universityofgalway.finance.marketdata.domain.Quote;
import ie.universityofgalway.finance.marketdata.domain.MarketDataUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Optional;

/**
 * Exposes stock quotes through the /api/quotes endpoints.
 *
 * <p>Missing data produces HTTP 404; provider unavailability produces HTTP 503.</p>
 */
@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    /**
     * Creates the controller with its application service.
     *
     * @param quoteService service used to retrieve stock quotes
     */
    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /**
     * Returns a quote, including its provider timestamp, optional statistics and stale-data flag.
     *
     * @param symbol ticker from the URL; normalised by the application service
     * @return HTTP 200 with the quote, or HTTP 404 with a problem detail if no quote is available
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<Object> getQuote(@PathVariable String symbol) {
        Optional<Quote> result = quoteService.getQuote(symbol);

        //unknown ticker: reply 404 with standard error reply
        if (result.isEmpty()) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "No quote found for symbol: " + symbol);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
        }

        //Known ticker: copy the Quote into the public response shape
        Quote quote = result.get();
        QuoteResponse response = new QuoteResponse(
                quote.symbol(),
                quote.price(),
                quote.asOf(),
                quote.stale(),
                QuoteDetailResponse.from(quote.details())
        );

        //reply 200 OK with the JSON
        return ResponseEntity.ok(response);
    }

    /**
     * Converts a provider failure without a usable fallback into a service-unavailable response.
     *
     * @param exception domain failure carrying a client-safe message
     * @return HTTP 503 with a problem detail
     */
    @ExceptionHandler(MarketDataUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleMarketDataUnavailable(
            MarketDataUnavailableException exception
    ) {
        //create a standard error body using the safe domain-level message.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(problem);
    }


}
