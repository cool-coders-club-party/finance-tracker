package ie.universityofgalway.finance.users.application;

public record AccessToken(String value, long expiresInSeconds) {
}
