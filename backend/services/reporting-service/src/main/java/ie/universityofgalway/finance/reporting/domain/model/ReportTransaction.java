package ie.universityofgalway.finance.reporting.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReportTransaction(
        // Declare data needed by transactions part of the report
        BigDecimal amount,
        String currency,
        String category,
        LocalDateTime occurredAt) {
}