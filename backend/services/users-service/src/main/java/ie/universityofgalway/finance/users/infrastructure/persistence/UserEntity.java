package ie.universityofgalway.finance.users.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * JPA mapping of the users table. Persistence only.
 * The rest of the application works with the domain User record.
 */
@Entity
@Table(name = "users") // name the table "users". "user" is a reserved word in Postgres
public class UserEntity {

    @Id // primary key. No @GeneratedValue as AuthService already generates the UUID
    private UUID id;

    // @Column used to define constraints, and mapping details
    @Column(nullable = false, unique = true) // unique = true makes the database refuse duplicate emails
    private String email;

    /*
     * BCrypt hash has 60 characters. A varchar(100) column holding 60 chars takes up the same amount of space as a varchar(60)
     * Changing this annotation later will not change an existing table.
     * To widen do: ALTER TABLE users ALTER COLUMN password_hash TYPE varchar(255)
     */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /**
     * The JPA spec requires all Entity classes to have a default no-arg constructor which can be public or protected.
     * As a constructor has been defined below, we need to provide a no-arg constructor explicitly.
     */
    protected UserEntity(){
        // this constructor is used by Hibernate to create instances of this entity using Reflection.
    }

    public UserEntity(UUID id, String email, String passwordHash){
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
}
