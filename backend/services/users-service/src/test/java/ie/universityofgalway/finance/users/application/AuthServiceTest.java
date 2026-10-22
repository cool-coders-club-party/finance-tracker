package ie.universityofgalway.finance.users.application;

import ie.universityofgalway.finance.users.domain.InvalidCredentialsException;
import ie.universityofgalway.finance.users.domain.User;
import ie.universityofgalway.finance.users.domain.UserRepository;
import ie.universityofgalway.finance.users.domain.EmailAlreadyRegisteredException;

import io.jsonwebtoken.security.Password;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class AuthServiceTest {

    private UserRepository users;
    private AuthService service;
    private PasswordHasher hasher;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        hasher = mock(PasswordHasher.class);
        // whenever the hash() method of BCryptPasswordHasher is invoked it will return "hashed" as the output
        // this will be considered the correct output and will be checked for in tests
        when(hasher.hash(anyString())).thenReturn("hashed");
        service = new AuthService(users, hasher);
    }

    /* REGISTER TESTS */
    @Test
    void registerSavesNewUserWithGivenEmail() {
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.register("test@example.com", "12345678");

        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.id()).isNotNull();
    }

    @Test
    void registerWithExistingEmailThrowsAndDoesNotSave() {
        when(users.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register("test@example.com", "12345678"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(users, never()).save(any());
    }

    @Test
    void registerTrimsAndLowercasesEmail() {
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.register("  Test@Example.COM ", "12345678");

        assertThat(result.email()).isEqualTo("test@example.com");
        verify(users).existsByEmail("test@example.com");
    }

    @Test
    void registerSavesHashAndNotRawPassword(){
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // AuthService.register() returns a User object with an email, hashed password and unique ID
        User result = service.register("test@example.com", "12345678");

        // service stub behavior, defined in setUp() returns "hashed" instead of an actual hashed value.
        assertThat(result.passwordHash()).isEqualTo("hashed");
        assertThat(result.passwordHash()).isNotEqualTo("12345678");
    }

    /* LOGIN TESTS */
    @Test
    void validLoginReturnsUser(){
        // create user
        User stored = new User(UUID.randomUUID(), "test@example.com", "hashed");

        // return Optional of the user we just made (i.e. simulate a successful registration)
        // and insert this user into the mock database
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(stored));

        // when matches() is called with these two parameters, then return true
        when(hasher.matches("12345678", "hashed")).thenReturn(true);

        // attempt login with valid credentials, this will return a User object
        User result = service.login("test@example.com", "12345678");

        // ensure that this User is equal to the one registered
        assertThat(result).isEqualTo(stored);
    }

    @Test
    void loginWithWrongPasswordThrows(){
        User stored = new User(UUID.randomUUID(), "test@example.com", "hashed");
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(stored));
        when(hasher.matches("wrong-password", "hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> service.login("test@example.com", "wrong-password"));

    }

    @Test
    void loginWithUnknownEmailThrows(){
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> service.login("test@example.com", "12345678"));
    }

    @Test
    void loginNormalisesEmail(){
        User stored = new User(UUID.randomUUID(), "test@example.com", "hashed");
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(stored));
        when(hasher.matches("12345678", "hashed")).thenReturn(true);

        User result = service.login("  Test@Example.COM ", "12345678");

        assertThat(result).isEqualTo(stored);
    }
}