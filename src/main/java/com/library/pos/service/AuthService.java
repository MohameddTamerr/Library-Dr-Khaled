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
            User owner = new User("admin", "admin", Role.OWNER, "System Owner", 0.0, "Library Address", "0000000000",
                    0.0);
            userRepository.save(owner);
        }
    }

    public User authenticate(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user.get();
        }
        return null;
    }
}
