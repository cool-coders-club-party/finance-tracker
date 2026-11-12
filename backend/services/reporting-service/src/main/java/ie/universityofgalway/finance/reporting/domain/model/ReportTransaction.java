package ie.universityofgalway.finance.reporting.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReportTransaction(
        BigDecimal amount,
        String currency,
        String category,
        LocalDateTime occurredAt) {
}