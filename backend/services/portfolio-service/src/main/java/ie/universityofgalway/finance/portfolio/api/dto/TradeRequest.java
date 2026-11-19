package ie.universityofgalway.finance.portfolio.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

// Validation annotations run BEFORE this ever reaches the controller body -
// Spring rejects an invalid request with a 400 automatically.
public record TradeRequest(

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{1,5}$", message = "Ticker must be 1-5 letters")
        String ticker,

        @Positive(message = "Quantity must be greater than zero")
        BigDecimal quantity,

        // Optional, for recording a purchase made earlier (buys only).
        // Send both or neither; with neither, the trade uses the live market price and time.
        @Positive(message = "Price must be greater than zero")
        BigDecimal price,

        Instant executedAt
) {
    // Keeps simple "trade at market" callers (and tests) short.
    public TradeRequest(String ticker, BigDecimal quantity) {
        this(ticker, quantity, null, null);
    }
}

