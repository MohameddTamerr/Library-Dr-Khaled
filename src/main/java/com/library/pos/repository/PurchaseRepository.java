package com.library.pos.repository;

import com.library.pos.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findBySupplier_IdOrderByPurchaseDateDesc(Long supplierId);
}
