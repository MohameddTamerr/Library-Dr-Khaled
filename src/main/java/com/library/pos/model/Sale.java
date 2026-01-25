package com.library.pos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
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

    public Sale() {
    }

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

    // Getters
    public Long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getItemName() {
        return itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public SaleStatus getStatus() {
        return status;
    }

    public User getWorker() {
        return worker;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Product getProduct() {
        return product;
    }

    public String getNotes() {
        return notes;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setStatus(SaleStatus status) {
        this.status = status;
    }

    public void setWorker(User worker) {
        this.worker = worker;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
