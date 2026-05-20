package com.adrplatform.auth.security;

import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

@Component
@RequestScope
public class UserContext {

    private User cachedUser;

    public User getUser(UUID userId, UserRepository userRepository) {
        if (cachedUser == null) {
            cachedUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        }
        return cachedUser;
    }

    public void set(User user) {
        this.cachedUser = user;
    }
}
