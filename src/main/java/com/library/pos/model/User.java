package com.library.pos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "salary")
    private Double hourlyRate;

    @Column(name = "phone")
    private String phoneNumber;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "withdrawal_limit")
    private Double salaryLimit;

    @Transient
    private Double currentWithdrawal;

    public User() {
    }

    public User(String username, String password, Role role, String fullName, Double hourlyRate,
            String phoneNumber, Double salaryLimit) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.hourlyRate = hourlyRate;
        this.phoneNumber = phoneNumber;
        this.salaryLimit = salaryLimit;
        this.currentWithdrawal = 0.0;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public Double getHourlyRate() {
        return hourlyRate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Double getSalaryLimit() {
        return salaryLimit;
    }

    public Double getCurrentWithdrawal() {
        return currentWithdrawal;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setSalaryLimit(Double salaryLimit) {
        this.salaryLimit = salaryLimit;
    }

    public void setCurrentWithdrawal(Double currentWithdrawal) {
        this.currentWithdrawal = currentWithdrawal;
    }
}
