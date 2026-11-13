package ie.universityofgalway.finance.users.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/auth/login}
 *
 * <p>The constraints on each component are checked when the controller parameter is annotated with {@code @Valid}
 * If any fail, Spring rejects the request with {@code 400 Bad Request} before the controller method runs.</p>
 *
 * <p>{@code @Email} only checks of the String follows the format {@code someText@moreText}, it doesn't check for
 * a real top-level domain or for a dot. The app should send a verification email to follow up on registration.</p>
 *
 * @param email the email address of the account (must not be blank)
 * @param password the plaintext account password input by the user (must not be blank)
 */
public record LoginRequest (@NotBlank @Email String email,
                            @NotBlank String password){
}
