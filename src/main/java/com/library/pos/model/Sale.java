package com.library.pos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sales")
@Data
@NoArgsConstructor
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus status;

    @ManyToOne
    @JoinColumn(name = "worker_id")
    private User worker;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(length = 500)
    private String notes;

    public Sale(LocalDateTime timestamp, String itemName, Integer quantity, Double totalAmount, SaleStatus status,
            User worker, String notes) {
        this.timestamp = timestamp;
        this.itemName = itemName;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = status;
        this.worker = worker;
        this.notes = notes;
    }
}
