package ie.universityofgalway.finance.marketdata.api;

import ie.universityofgalway.finance.marketdata.application.StockMoversService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StockMoversController {
    private final StockMoversService stockMoversService;

    public StockMoversController(StockMoversService stockMoversService) {
        this.stockMoversService = stockMoversService;
    }

    @GetMapping("/api/rankings/movers")
    public ResponseEntity<?> getMovers() {
        var ranking = stockMoversService.getMovers();
        if (ranking.isPresent()) {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ranking.get());
        }

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "Stock movers are temporarily unavailable"
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore())
                .body(problem);
    }
}
