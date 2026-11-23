package ie.universityofgalway.finance.reporting.domain.model;

import java.math.BigDecimal;

public record ReportInvestment(
        // Declare data needed by portfolio part of the report.
        String ticker,
        BigDecimal quantity,
        BigDecimal amountInvested,
        BigDecimal currentPrice) {
}