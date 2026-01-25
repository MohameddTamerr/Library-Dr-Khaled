package com.library.pos.repository;

import com.library.pos.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByBarcode(String barcode);

    List<Product> findByBarcodeContainingIgnoreCaseOrNameContainingIgnoreCase(String barcode, String name);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL")
    List<String> findDistinctCategories();
}
