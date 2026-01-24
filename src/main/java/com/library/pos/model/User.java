package com.library.pos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // In real app, this should be hashed

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String fullName;
    private Double hourlyRate; // For workers

    // New fields for Worker Management
    private String address;
    private String phoneNumber;
    private Double salaryLimit; // Max amount they can withdraw
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
