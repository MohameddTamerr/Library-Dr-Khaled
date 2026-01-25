package com.library.pos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // In real app, this should be hashed

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "salary")
    private Double hourlyRate; // Mapped to salary column

    @Column(name = "phone")
    private String phoneNumber;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // Not in the SQL schema. Keep transient so JPA doesn't expect columns.
    @Transient
    private String address;
    @Transient
    private Double salaryLimit; // Max amount they can withdraw
    @Transient
    private Double currentWithdrawal; // Amount currently withdrawn this month

    // Full constructor
    public User(String username, String password, Role role, String fullName, Double hourlyRate, String address,
            String phoneNumber, Double salaryLimit) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.hourlyRate = hourlyRate;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.salaryLimit = salaryLimit;
        this.currentWithdrawal = 0.0;
    }
}
