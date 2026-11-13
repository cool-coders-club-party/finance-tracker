package ie.universityofgalway.finance.users.application;

import java.util.UUID;

/**
 * The result of rotating a refresh token with {@link RefreshTokenService#rotate(String)}.
 *
 * <p>The controller uses {@code userId} to load the user and issue a new access token, and
 * returns {@code refreshToken} to the client to replace the one it just used.
 *
 * @param userId       the id of the user who owns the rotated token
 * @param refreshToken the new raw (unhashed) refresh token, which the client must store in place of the old one
 */
public record RotatedToken(UUID userId, String refreshToken) {
}
