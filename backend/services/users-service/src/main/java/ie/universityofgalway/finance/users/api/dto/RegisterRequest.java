package ie.universityofgalway.finance.users.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/auth/register}.
 *
 * <p>Carries the credentials a new user submits when creating an account. Spring
 * converts the incoming JSON into this object and, because the controller parameter
 * is marked {@code @Valid}, checks the constraints below before the controller method
 * runs. A failed check produces a {@code 400 Bad Request}.
 *
 * @param email    the address the user will log in with; must be a well-formed email address
 * @param password the plain-text password chosen by the user, 8 to 72 characters;
 *                 it is hashed before storage and must never be logged or returned
 */
public record RegisterRequest(@NotBlank @Email String email,
                              @NotBlank @Size(min = 8, max = 72) String password) {
}
