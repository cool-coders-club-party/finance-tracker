package ie.universityofgalway.finance.marketdata.domain;

import java.math.BigDecimal;
import java.time.Instant;

//record is a short way to write a class that only holds values
public record Quote (String symbol, BigDecimal price, Instant asOf) {}

