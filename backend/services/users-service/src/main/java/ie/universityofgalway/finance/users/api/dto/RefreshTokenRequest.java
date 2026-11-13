package ie.universityofgalway.finance.users.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/auth/refresh} and {@code POST /api/auth/logout}.
 *
 * <p>Carries the refresh token the client received from login or from a previous refresh.
 * The constraint is checked when the controller parameter is annotated with {@code @Valid}.
 * If it fails, Spring rejects the request with {@code 400 Bad Request} before the
 * controller method runs.
 *
 * @param refreshToken the raw refresh token issued to the client (must not be blank)
 */
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
