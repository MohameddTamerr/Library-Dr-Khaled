package com.library.pos.service;

import com.library.pos.model.Role;
import com.library.pos.model.User;
import com.library.pos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllWorkers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.WORKER)
                .collect(Collectors.toList());
    }

    public User saveWorker(User user) {
        user.setRole(Role.WORKER);
        // In a real app, hash the password here
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
