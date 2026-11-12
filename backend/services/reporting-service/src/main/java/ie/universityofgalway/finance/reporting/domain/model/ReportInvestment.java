package ie.universityofgalway.finance.reporting.domain.model;

import java.math.BigDecimal;

public record ReportInvestment(
        String ticker,
        BigDecimal quantity,
        BigDecimal amountInvested,
        BigDecimal currentPrice) {
}