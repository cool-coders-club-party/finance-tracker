package ie.universityofgalway.finance.users.application;

import ie.universityofgalway.finance.users.domain.User;
import ie.universityofgalway.finance.users.domain.UserRepository;
import ie.universityofgalway.finance.users.domain.EmailAlreadyRegisteredException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        service = new AuthService(users);
    }

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
}