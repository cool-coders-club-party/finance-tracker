package ie.universityofgalway.finance.marketdata.api;

import ie.universityofgalway.finance.marketdata.application.TopStocksService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the manually maintained top-five market-cap snapshot at /api/rankings/top-stocks.
 */
@RestController
public class TopStocksController {
    private final TopStocksService topStocksService;

    /**
     * Creates the ranking controller.
     *
     * @param topStocksService service supplying the ranking
     */
    public TopStocksController(TopStocksService topStocksService) {
        this.topStocksService = topStocksService;
    }

    /**
     * Returns the deployed snapshot without browser caching.
     *
     * <p>The snapshot remains available until it is manually replaced and redeployed.</p>
     *
     * @return HTTP 200 with the snapshot, or HTTP 503 if it could not be loaded or validated
     */
    @GetMapping("/api/rankings/top-stocks")
    public ResponseEntity<?> getTopStocks() {
        var snapshot = topStocksService.getTopStocks();
        if (snapshot.isEmpty()) {
            var problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.SERVICE_UNAVAILABLE, "Ranking unavailable"
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .cacheControl(CacheControl.noStore())
                    .body(problem);
        }

        // Serve the deployed snapshot rather than a browser's previously cached version.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(snapshot.get());
    }
}
