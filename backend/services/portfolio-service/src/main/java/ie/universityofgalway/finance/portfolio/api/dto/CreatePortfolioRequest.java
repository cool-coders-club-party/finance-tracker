package ie.universityofgalway.finance.portfolio.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/portfolios}.
 *
 * <p>Validated before the controller runs; a failure becomes {@code 400 VALIDATION_ERROR}.</p>
 *
 * @param name the portfolio's display name: required, at most 255 characters
 */
public record CreatePortfolioRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be 255 characters or fewer")
        String name
){}