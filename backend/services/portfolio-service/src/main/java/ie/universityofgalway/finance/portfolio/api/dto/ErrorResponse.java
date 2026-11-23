package ie.universityofgalway.finance.portfolio.api.dto;

/**
 * JSON body of every error returned by {@link ie.universityofgalway.finance.portfolio.api.controller.GlobalExceptionHandler}.
 *
 * <p>Note that 401 responses use {@code code} instead of {@code error}; see
 * {@link ie.universityofgalway.finance.portfolio.infrastructure.config.JsonAuthenticationEntryPoint}.</p>
 *
 * @param error   a stable, machine-readable code such as {@code INSUFFICIENT_SHARES}
 * @param message a human-readable explanation, safe to show to the user
 */
public record ErrorResponse(String error, String message) {}
