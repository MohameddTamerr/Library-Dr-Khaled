package com.library.pos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "worker_salary_summary")
@Data
@NoArgsConstructor
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
}
