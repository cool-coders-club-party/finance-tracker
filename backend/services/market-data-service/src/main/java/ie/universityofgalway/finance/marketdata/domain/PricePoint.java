package ie.universityofgalway.finance.marketdata.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A stock's closing price on one trading date.
 *
 * @param date trading date
 * @param close closing price in the instrument's quoted currency
 */
public record PricePoint(LocalDate date, BigDecimal close) {
}