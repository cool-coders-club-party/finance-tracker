package ie.universityofgalway.finance.users.infrastructure.persistence;

import ie.universityofgalway.finance.users.domain.User;
import ie.universityofgalway.finance.users.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary storage that keeps users in memory.
 * All data is lost when the app stops.
 * <p>
 * A ConcurrentHashMap is a HashMap that is safe when several requests
 * arrive at once
 * <p>
 * {@code @Repository} tells Spring to instantiate this class on boot
 * and make it available to any classes that look for it.
 */
@Repository
public class InMemoryUserRepository implements UserRepository{
    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();

    @Override
    public User save(User user){
        usersByEmail.put(user.email(), user);
        return user;
    }

    @Override
    public boolean existsByEmail(String email){
        return usersByEmail.containsKey(email);
    }
}
