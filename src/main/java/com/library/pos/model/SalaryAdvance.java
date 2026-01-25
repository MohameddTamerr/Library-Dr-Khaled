package com.library.pos.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "salary_advances")
public class SalaryAdvance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "advance_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "worker_id")
    private User worker;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "advance_date")
    private LocalDate advanceDate;

    @Column
    private String reason;

    @Column(name = "is_deducted")
    private Boolean isDeducted;

    public SalaryAdvance() {
    }

    // Getters
    public Long getId() {
        return id;
    }

    public User getWorker() {
        return worker;
    }

    public Double getAmount() {
        return amount;
    }

    public LocalDate getAdvanceDate() {
        return advanceDate;
    }

    public String getReason() {
        return reason;
    }

    public Boolean getIsDeducted() {
        return isDeducted;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setWorker(User worker) {
        this.worker = worker;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setAdvanceDate(LocalDate advanceDate) {
        this.advanceDate = advanceDate;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setIsDeducted(Boolean isDeducted) {
        this.isDeducted = isDeducted;
    }
}
