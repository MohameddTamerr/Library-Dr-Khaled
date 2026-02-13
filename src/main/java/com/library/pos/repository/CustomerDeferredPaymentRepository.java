package com.library.pos.repository;

import com.library.pos.model.CustomerDeferredPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CustomerDeferredPaymentRepository extends JpaRepository<CustomerDeferredPayment, Long> {

    List<CustomerDeferredPayment> findByCustomer_IdOrderByPaymentDateDesc(Long customerId);

    @Query("SELECT p.customer.id, SUM(p.amount) FROM CustomerDeferredPayment p WHERE p.customer.id IN :customerIds GROUP BY p.customer.id")
    List<Object[]> sumAmountsByCustomerIds(@Param("customerIds") List<Long> customerIds);

    @Query("SELECT SUM(p.amount) FROM CustomerDeferredPayment p WHERE p.paymentDate BETWEEN :start AND :end")
    BigDecimal calculateDailyTotal(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(p.amount) FROM CustomerDeferredPayment p WHERE p.paymentDate BETWEEN :start AND :end AND p.method = 'CASH'")
    BigDecimal calculateDailyCashTotal(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM CustomerDeferredPayment p WHERE p.customer.id = :customerId")
    BigDecimal sumByCustomerId(@Param("customerId") Long customerId);
}
