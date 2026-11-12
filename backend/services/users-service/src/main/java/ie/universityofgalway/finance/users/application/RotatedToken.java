package ie.universityofgalway.finance.users.application;

import java.util.UUID;

public record RotatedToken(UUID userId, String refreshToken) {
}
