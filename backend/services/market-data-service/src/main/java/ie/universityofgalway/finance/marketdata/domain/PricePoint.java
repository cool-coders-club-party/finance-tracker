package ie.universityofgalway.finance.marketdata.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

// One daily closing price for a stock.
public record PricePoint(LocalDate date, BigDecimal close) {
}