package com.library.pos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "worker_salary_summary")
public class WorkerSalarySummary {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "salary")
    private Double salary;

    @Column(name = "total_advance")
    private Double totalAdvance;

    @Column(name = "net_salary")
    private Double netSalary;

    public WorkerSalarySummary() {
    }

    // Getters
    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public Double getSalary() {
        return salary;
    }

    public Double getTotalAdvance() {
        return totalAdvance;
    }

    public Double getNetSalary() {
        return netSalary;
    }

    // Setters
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    public void setTotalAdvance(Double totalAdvance) {
        this.totalAdvance = totalAdvance;
    }

    public void setNetSalary(Double netSalary) {
        this.netSalary = netSalary;
    }
}
