package com.library.pos.repository;

import com.library.pos.model.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {
    @Query("SELECT SUM(p.amount) FROM SupplierPayment p WHERE p.paymentDate BETWEEN :start AND :end")
    BigDecimal calculateDailyTotal(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(p.amount) FROM SupplierPayment p WHERE p.paymentDate BETWEEN :start AND :end AND p.method = 'CASH'")
    BigDecimal calculateDailyCashTotal(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<SupplierPayment> findBySupplier_IdOrderByPaymentDateDesc(Long supplierId);
}
