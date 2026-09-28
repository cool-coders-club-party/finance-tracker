Compiled to make sure everything is AY OKAY
```bash
./mvnw -pl services/market-data-service compile
```
Run tests to make sure they are working ok
```bash
./mvnw -pl services/market-data-service test
```
Created QuoteService in `/application/`

---
## Nice explanation from Claude  about endpoint

An endpoint has three parts, and each lives in a different place:

| Part                             | Example                          | Where it's defined            |
| -------------------------------- | -------------------------------- | ----------------------------- |
| **The address**: method + URL    | `GET /api/quotes/AAPL`           | `QuoteController` (next step) |
| **The work**: what happens       | clean the symbol, find the price | `QuoteService` (done)         |
| **The reply**: shape of the JSON | `{ "symbol", "price", "asOf" }`  | `QuoteResponse` (this step)   |

Think of it like a shop counter. The controller is the counter where people ask, the service is the stockroom where the item gets found, and `QuoteResponse` is the box the item is handed over in.

---

`/application/QuoteService`
```java
package ie.universityofgalway.finance.marketdata.application;

import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import ie.universityofgalway.finance.marketdata.domain.Quote;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

// Tells Spring to create one QuoteService when the app starts
@Service
public class QuoteService {

    // Where prices come from (the stub, for now)
    private final MarketDataProvider marketDataProvider;

    // Spring passes in the provider automatically
    public QuoteService(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }

    // Looks up a quote. Returns empty if the symbol is blank or unknown
    public Optional<Quote> getQuote(String rawSymbol) {

        // Nothing typed at all
        if (rawSymbol == null) {
            return Optional.empty();
        }

        // Remove spaces from both ends: "  aapl " becomes "aapl"
        String trimmed = rawSymbol.trim();

        // Only spaces were typed
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        // Uppercase with standard English rules: "aapl" becomes "AAPL"
        // Locale.ROOT is Java's language-neutral locale, so uppercasing
        // behaves the same regardless of the computer's regional settings
        String symbol = trimmed.toUpperCase(Locale.ROOT);

        // Ask the provider for the price
        return marketDataProvider.findQuote(symbol);
    }
}
```
---
`/api/QuoteResponses`

```java
package ie.universityofgalway.finance.marketdata.api;

import java.math.BigDecimal;
import java.time.Instant;

// The JSON returned by GET /api/quotes/{symbol}.
// Other services depend on these field names, so agree changes with the team first.
public record QuoteResponse(
        String symbol,     // e.g. "AAPL"
        BigDecimal price,  // exact money value (double can't store money precisely)
        Instant asOf       // when the price was fetched, so callers can see how fresh it is
) {
}
```


`/api/QuoteController.java`

```java 
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

// Handles web requests and returns JSON
@RestController
// All URLs in this class start with /api/quotes
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    // Spring passes in the QuoteService automatically
    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    // Runs for GET /api/quotes/{symbol}, e.g. /api/quotes/AAPL
    // @PathVariable copies the {symbol} part of the URL into the parameter
    @GetMapping("/{symbol}")
    public ResponseEntity<Object> getQuote(@PathVariable String symbol) {

        Optional<Quote> result = quoteService.getQuote(symbol);

        // Unknown ticker: reply 404 with a standard error body
        if (result.isEmpty()) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "No quote found for symbol: " + symbol);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
        }

        // Known ticker: copy the Quote into the public response shape
        Quote quote = result.get();
        QuoteResponse response = new QuoteResponse(quote.symbol(), quote.price(), quote.asOf());

        // Reply 200 OK with the JSON
        return ResponseEntity.ok(response);
    }
}
```
- **`ResponseEntity`** lets you set the status code (200 or 404) as well as the body. It's `<Object>` because the body is a `QuoteResponse` on success but an error on failure.
- **`ProblemDetail`** is Spring's standard error format. It becomes JSON like `{"status":404,"title":"Not Found","detail":"No quote found for symbol: NOPE"}`. If every service uses it, the frontend handles all errors the same way.

---

```bash
./mvnw -pl services/market-data-service spring-boot:run
```

```bash
curl -i http://localhost:8084/api/quotes/AAPL
curl -i http://localhost:8084/api/quotes/aapl
curl -i http://localhost:8084/api/quotes/NOPE
```

![](../../Screenshots/Screenshot%202026-09-21%20at%2011.06.04.png)

All returned as expected, it is functioning properly! (200, 200 and 404)

