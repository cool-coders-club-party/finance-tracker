package ie.universityofgalway.finance.users.domain;

import java.util.UUID;

/**
 * A registered user of the finance tracker
 *
 * @param id unique identifier, generated when the user registers
 * @param email the address the user logs in with, stored in lowercase
 * @param passwordHash hashed password
 */
public record User(UUID id, String email, String passwordHash) {
    /**
     * Overriding toString() to omit the hashed password
     * @return fields of the User class, omitting the hashed password
     */
    @Override
    public String toString(){
        return "User[id= " + id + ", email= " + email + "]";
    }
}
