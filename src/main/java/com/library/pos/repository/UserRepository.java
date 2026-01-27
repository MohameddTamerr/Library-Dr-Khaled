package com.library.pos.repository;

import com.library.pos.model.User;
import com.library.pos.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByRole(com.library.pos.model.Role role);

    long countByRole(Role role);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(u.updatedAt) FROM User u WHERE u.role = :role")
    java.time.LocalDateTime findLatestUpdateByRole(@org.springframework.data.repository.query.Param("role") Role role);
}
