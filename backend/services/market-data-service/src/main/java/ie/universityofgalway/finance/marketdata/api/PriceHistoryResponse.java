package ie.universityofgalway.finance.marketdata.api;

import ie.universityofgalway.finance.marketdata.domain.PricePoint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Public JSON shape for daily history.
public record PriceHistoryResponse(List<Point> points) {

    public static PriceHistoryResponse from(List<PricePoint> history) {
        // Keep the API response separate from the domain record.
        List<Point> points = history.stream()
                .map(price -> new Point(price.date(), price.close()))
                .toList();

        return new PriceHistoryResponse(points);
    }

    public record Point(LocalDate date, BigDecimal close) {
    }
}