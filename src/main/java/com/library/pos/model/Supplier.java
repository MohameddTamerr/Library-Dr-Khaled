package com.library.pos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 8)
    private SupplierStatus status = SupplierStatus.ACTIVE;

    @Column(name = "total_purchases", nullable = false)
    private BigDecimal totalPurchases = BigDecimal.ZERO;

    @Column(name = "total_paid", nullable = false)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(name = "balance_due", nullable = false)
    private BigDecimal balanceDue = BigDecimal.ZERO;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Supplier() {
    }

    public Supplier(Long id, String name, String phone, String email, String address, SupplierStatus status,
            BigDecimal totalPurchases, BigDecimal totalPaid, BigDecimal balanceDue, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.status = status;
        this.totalPurchases = totalPurchases;
        this.totalPaid = totalPaid;
        this.balanceDue = balanceDue;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public SupplierStatus getStatus() {
        return status;
    }

    public void setStatus(SupplierStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalPurchases() {
        return totalPurchases;
    }

    public void setTotalPurchases(BigDecimal totalPurchases) {
        this.totalPurchases = totalPurchases;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(BigDecimal totalPaid) {
        this.totalPaid = totalPaid;
    }

    public BigDecimal getBalanceDue() {
        return balanceDue;
    }

    public void setBalanceDue(BigDecimal balanceDue) {
        this.balanceDue = balanceDue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    private void ensureDefaults() {
        if (status == null) {
            status = SupplierStatus.ACTIVE;
        }
        if (totalPurchases == null) {
            totalPurchases = BigDecimal.ZERO;
        }
        if (totalPaid == null) {
            totalPaid = BigDecimal.ZERO;
        }
        if (balanceDue == null) {
            balanceDue = BigDecimal.ZERO;
        }
    }

    @Override
    public String toString() {
        return name + (phone != null && !phone.isBlank() ? " | " + phone : "");
    }
}
