package com.library.pos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_payments")
public class SupplierPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method = PaymentMethod.CASH;

    @Column(name = "method_display", length = 32)
    private String methodDisplay;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public SupplierPayment() {
    }

    public SupplierPayment(Long id, Supplier supplier, BigDecimal amount, LocalDateTime paymentDate,
            PaymentMethod method, String methodDisplay, String notes) {
        this.id = id;
        this.supplier = supplier;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.method = method;
        this.methodDisplay = methodDisplay;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getNotes() {
        return notes;
    }

    public String getMethodDisplay() {
        return methodDisplay;
    }

    public void setMethodDisplay(String methodDisplay) {
        this.methodDisplay = methodDisplay;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @PrePersist
    private void applyDefaults() {
        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
        if (method == null) {
            method = PaymentMethod.CASH;
        }
        if (methodDisplay == null || methodDisplay.isBlank()) {
            methodDisplay = switch (method) {
                case CASH -> "Cash";
                case BANK -> "Bank";
                default -> "Other";
            };
        }
    }
}
