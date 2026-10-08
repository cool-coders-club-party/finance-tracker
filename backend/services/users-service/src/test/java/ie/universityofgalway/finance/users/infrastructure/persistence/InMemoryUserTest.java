package ie.universityofgalway.finance.users.infrastructure.persistence;

import ie.universityofgalway.finance.users.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryUserRepositoryTest {

    private InMemoryUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();   // empty and fresh for every test
    }

    @Test
    void existsByEmailIsFalseWhenNothingHasBeenSaved() {
        assertFalse(repository.existsByEmail("sam@example.com"));
    }

    @Test
    void existsByEmailIsTrueAfterSaving() {
        repository.save(new User(UUID.randomUUID(), "sam@example.com"));

        assertTrue(repository.existsByEmail("sam@example.com"));
    }

    @Test
    void saveReturnsTheUserItWasGiven() {
        User user = new User(UUID.randomUUID(), "sam@example.com");

        assertEquals(user, repository.save(user));
    }
}