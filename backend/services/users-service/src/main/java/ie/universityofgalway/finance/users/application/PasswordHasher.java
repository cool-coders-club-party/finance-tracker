package ie.universityofgalway.finance.users.application;

/**
 * Hashes passwords and checks a password against a stored hash.
 *
 * <p>This interface keeps the choice of hashing algorithm out of the application layer.
 * {@link AuthService} depends only on this contract, so the implementation (e.g. BCrypt)
 * can be changed without touching it.
 */
public interface PasswordHasher {
    /**
     * Hashes a plain text password so that it can be stored safely.
     *
     * <p>The result is a one-way hash and cannot be converted back to the password.
     *
     * @param password the plain text password to hash
     * @return the hash to store in place of the password
     */
    String hash(String password);

    /**
     * Checks whether a plain text password corresponds to a stored hash.
     *
     * @param password the plain text password to check, such as one submitted at login
     * @param hash     the stored hash to compare it against
     * @return {@code true} if the password matches the hash, otherwise {@code false}
     */
    boolean matches(String password, String hash);
}
