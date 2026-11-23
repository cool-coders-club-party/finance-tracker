package ie.universityofgalway.finance.portfolio.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request body for {@code POST /api/portfolios/{id}/buy} and {@code /sell}.
 *
 * <p>Validation annotations run before the controller body; Spring rejects an invalid request
 * with {@code 400 VALIDATION_ERROR} automatically. Rules that involve more than one field (send
 * both {@code price} and {@code executedAt}, or neither) are checked in
 * {@link ie.universityofgalway.finance.portfolio.application.service.PortfolioService}.</p>
 *
 * @param ticker     stock symbol, 1-5 letters in any case
 * @param quantity   number of shares, greater than zero; fractional shares are allowed
 * @param price      buys only, optional: price per share actually paid, for recording an earlier purchase
 * @param executedAt buys only, optional: when that earlier purchase happened (ISO-8601, e.g.
 *                   {@code 2026-11-17T14:32:00Z})
 */
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
    /**
     * Creates a request to trade at the market price, with no price or time.
     * Keeps simple callers and tests short.
     *
     * @param ticker   stock symbol
     * @param quantity number of shares
     */
    public TradeRequest(String ticker, BigDecimal quantity) {
        this(ticker, quantity, null, null);
    }
}

