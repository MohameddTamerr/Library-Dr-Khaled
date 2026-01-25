package com.library.pos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "salary_advances")
@Data
@NoArgsConstructor
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
}
