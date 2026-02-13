package com.library.pos.service;

import com.library.pos.model.Role;
import com.library.pos.model.User;
import com.library.pos.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;

    @Autowired
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Initialize default owner if none exists.
     */
    @PostConstruct
    public void init() {
        if (!userRepository.existsByRole(Role.OWNER)) {
            System.out.println("Creating default Owner account...");
            User owner = new User("admin", "admin", Role.OWNER, "System Owner", 0.0, "0000000000",
                    0.0);
            userRepository.save(owner);
        }
    }

    public User authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        String normalizedUsername = username.trim();
        String normalizedPassword = password.trim();
        if (normalizedUsername.isEmpty() || normalizedPassword.isEmpty()) {
            return null;
        }

        Optional<User> user = userRepository.findByUsername(normalizedUsername);
        if (user.isEmpty()) {
            user = userRepository.findAll().stream()
                    .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(normalizedUsername))
                    .findFirst();
        }

        if (user.isPresent() && user.get().getPassword() != null
                && user.get().getPassword().trim().equals(normalizedPassword)) {
            return user.get();
        }
        return null;
    }
}
