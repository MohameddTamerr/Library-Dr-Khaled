package com.library.pos.repository;

import com.library.pos.model.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {
}
