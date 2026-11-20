package ie.universityofgalway.finance.marketdata.api;

import ie.universityofgalway.finance.marketdata.domain.PricePoint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Public JSON representation of a stock's daily closing prices.
 *
 * @param points daily points in the order supplied by the history provider
 */
public record PriceHistoryResponse(List<Point> points) {

    /**
     * Maps domain prices into the public response without changing their order.
     *
     * @param history non-null list of daily closing prices
     * @return the mapped history response
     */
    public static PriceHistoryResponse from(List<PricePoint> history) {
        // Keep the API response separate from the domain record.
        List<Point> points = history.stream()
                .map(price -> new Point(price.date(), price.close()))
                .toList();

        return new PriceHistoryResponse(points);
    }

    /**
     * One closing price in a history response.
     *
     * @param date trading date
     * @param close closing price in the instrument's quoted currency
     */
    public record Point(LocalDate date, BigDecimal close) {
    }
}