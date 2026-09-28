package ie.universityofgalway.finance.marketdata.api;

import ie.universityofgalway.finance.marketdata.application.QuoteService;
import ie.universityofgalway.finance.marketdata.domain.Quote;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

//Handles web requests and returns JSON
@RestController
//All URLs in this class start with /api/quotes
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService){
        this.quoteService = quoteService;
    }

    //Runs for GET /api/quotes/{symbol}, e.g /api/quotes/AAPL
    //@PathVariable copies the {symbol} part of the URL into the parameter
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
        QuoteResponse response = new QuoteResponse(quote.symbol(), quote.price(), quote.asOf());

        //reply 200 OK with the JSON
        return ResponseEntity.ok(response);
    }

}
