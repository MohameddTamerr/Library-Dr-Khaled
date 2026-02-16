package com.library.pos.repository;

import com.library.pos.model.ProductBarcode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductBarcodeRepository extends JpaRepository<ProductBarcode, Long> {
    Optional<ProductBarcode> findByBarcodeIgnoreCase(String barcode);

    List<ProductBarcode> findByProductIdOrderByIdAsc(Long productId);
}
