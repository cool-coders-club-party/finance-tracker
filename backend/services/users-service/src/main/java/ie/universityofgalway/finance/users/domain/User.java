package ie.universityofgalway.finance.users.domain;

import java.util.UUID;

/**
 * A registered user of the finance tracker
 *
 * @param id unique identifier, generated when the user registers
 * @param email the address the user logs in with, stored in lowercase
 */
public record User(UUID id, String email) {
}
