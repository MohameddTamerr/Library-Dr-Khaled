package com.library.pos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String barcode;

    private String category;

    @Column(nullable = false)
    private Double cost;

    @Column(nullable = false)
    private Double sellPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer minStock;

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
