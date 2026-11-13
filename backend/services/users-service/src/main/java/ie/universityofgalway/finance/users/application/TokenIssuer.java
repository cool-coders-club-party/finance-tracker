package ie.universityofgalway.finance.users.application;

import ie.universityofgalway.finance.users.domain.User;

/**
 * Issues access tokens for authenticated users.
 *
 * <p>This interface keeps the details of how tokens are created, such as the format and
 * signing method, out of the application layer. The controller depends only on this
 * contract, so the implementation can change without touching it.
 */
public interface TokenIssuer {

    /**
     * Creates a new access token for the given user.
     *
     * <p>The token identifies the user to later requests, which present it in the
     * {@code Authorization: Bearer} header.
     *
     * @param user the authenticated user the token is issued for
     * @return the new {@link AccessToken}, containing the encoded token and its lifetime in seconds
     */
    AccessToken issue(User user);
}
