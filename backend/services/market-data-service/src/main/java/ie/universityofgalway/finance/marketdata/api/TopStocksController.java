package ie.universityofgalway.finance.marketdata.api;

import ie.universityofgalway.finance.marketdata.application.TopStocksService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TopStocksController {
    private final TopStocksService topStocksService;

    public TopStocksController(TopStocksService topStocksService) {
        this.topStocksService = topStocksService;
    }

    @GetMapping("/api/rankings/top-stocks")
    public ResponseEntity<?> getTopStocks() {
        var snapshot = topStocksService.getTopStocks();
        if (snapshot.isEmpty()) {
            var problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.SERVICE_UNAVAILABLE, "Fresh ranking unavailable"
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .cacheControl(CacheControl.noStore())
                    .body(problem);
        }

        // A cached response could bypass the service's expiry check.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(snapshot.get());
    }
}
