package ie.universityofgalway.finance.users.application;

import ie.universityofgalway.finance.users.domain.User;

public interface TokenIssuer {
    AccessToken issue(User user);
}
