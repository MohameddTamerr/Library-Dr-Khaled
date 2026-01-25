package com.library.pos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
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

    public Product(String name, String barcode, String category, Double cost, Double sellPrice,
                   Integer quantity, Integer minStock) {
        this.name = name;
        this.barcode = barcode;
        this.category = category;
        this.cost = cost;
        this.sellPrice = sellPrice;
        this.quantity = quantity;
        this.minStock = minStock;
    }
}
