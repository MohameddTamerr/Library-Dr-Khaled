package com.library.pos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a product that was wasted / broken (هالك).
 * Recording a wasted item subtracts its quantity from the product's stock
 * and is treated as a cost loss that reduces net profit.
 */
@Entity
@Table(name = "wasted_items")
public class WastedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wasted_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * The cost price at the time of recording, stored for historical accuracy.
     */
    @Column(name = "cost_per_unit", nullable = false)
    private Double costPerUnit;

    /**
     * Total cost loss = costPerUnit * quantity
     */
    @Column(name = "total_cost_loss", nullable = false)
    private Double totalCostLoss;

    @Column(name = "wasted_at", nullable = false)
    private LocalDateTime wastedAt;

    @Column(name = "notes")
    private String notes;

    @ManyToOne
    @JoinColumn(name = "worker_id")
    private User worker;

    public WastedItem() {
    }

    public WastedItem(Product product, Integer quantity, User worker, String notes) {
        this.product = product;
        this.productName = product.getName();
        this.quantity = quantity;
        this.costPerUnit = product.getCost();
        this.totalCostLoss = product.getCost() * quantity;
        this.wastedAt = LocalDateTime.now();
        this.worker = worker;
        this.notes = notes;
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Double getCostPerUnit() {
        return costPerUnit;
    }

    public Double getTotalCostLoss() {
        return totalCostLoss;
    }

    public LocalDateTime getWastedAt() {
        return wastedAt;
    }

    public String getNotes() {
        return notes;
    }

    public User getWorker() {
        return worker;
    }

    // --- Setters ---

    public void setId(Long id) {
        this.id = id;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setCostPerUnit(Double costPerUnit) {
        this.costPerUnit = costPerUnit;
    }

    public void setTotalCostLoss(Double totalCostLoss) {
        this.totalCostLoss = totalCostLoss;
    }

    public void setWastedAt(LocalDateTime wastedAt) {
        this.wastedAt = wastedAt;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setWorker(User worker) {
        this.worker = worker;
    }
}
