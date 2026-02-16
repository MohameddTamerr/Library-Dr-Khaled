package com.library.pos.repository;

import com.library.pos.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByBarcode(String barcode);

    Optional<Product> findByBarcodeIgnoreCase(String barcode);

    List<Product> findByBarcodeContainingIgnoreCaseOrNameContainingIgnoreCase(String barcode, String name);

    @Query("""
            SELECT DISTINCT p
            FROM Product p
            LEFT JOIN p.additionalBarcodes b
            WHERE lower(p.name) LIKE lower(concat('%', :term, '%'))
               OR lower(p.barcode) LIKE lower(concat('%', :term, '%'))
               OR lower(b.barcode) LIKE lower(concat('%', :term, '%'))
            """)
    List<Product> searchByAnyBarcodeOrName(@Param("term") String term);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL")
    List<String> findDistinctCategories();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.supplier FROM Product p WHERE p.supplier IS NOT NULL")
    List<String> findDistinctSuppliers();

    @org.springframework.data.jpa.repository.Query("SELECT MAX(p.updatedAt) FROM Product p")
    java.time.LocalDateTime findLatestUpdate();
}
