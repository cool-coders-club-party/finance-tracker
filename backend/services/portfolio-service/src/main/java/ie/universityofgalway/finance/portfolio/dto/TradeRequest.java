package ie.universityofgalway.finance.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// Validation annotations run BEFORE this ever reaches the controller body -
// Spring rejects an invalid request with a 400 automatically.
public record TradeRequest(

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{1,5}$", message = "Ticker must be 1-5 letters")
        String ticker,

        @Positive(message = "Quantity must be greater than zero")
        BigDecimal quantity
) {}
