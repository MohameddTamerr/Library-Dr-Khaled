package com.library.pos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "product_name", nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String barcode;

    @Column(name = "category")
    private String category;

    @Column(name = "supplier")
    private String supplier;

    @Column(name = "cost", nullable = false)
    private Double cost;

    @Column(name = "price", nullable = false)
    private Double sellPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "min_stock", nullable = false)
    private Integer minStock;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Product() {
    }

    public Product(String name, String barcode, String category, Double cost, Double sellPrice,
            Integer quantity, Integer minStock, String supplier) {
        this.name = name;
        this.barcode = barcode;
        this.category = category;
        this.cost = cost;
        this.sellPrice = sellPrice;
        this.quantity = quantity;
        this.minStock = minStock;
        this.supplier = supplier;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getCategory() {
        return category;
    }

    public String getSupplier() {
        return supplier;
    }

    public Double getCost() {
        return cost;
    }

    public Double getSellPrice() {
        return sellPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getMinStock() {
        return minStock;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public void setSellPrice(Double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setMinStock(Integer minStock) {
        this.minStock = minStock;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
