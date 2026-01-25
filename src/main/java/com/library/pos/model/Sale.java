package com.library.pos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime timestamp;

    @Transient
    private String itemName;

    @Transient
    private Integer quantity;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Transient
    private SaleStatus status;

    @ManyToOne
    @JoinColumn(name = "worker_id")
    private User worker;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Transient
    private Product product;

    @Transient
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
