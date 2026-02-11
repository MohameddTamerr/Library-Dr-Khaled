package com.library.pos.repository;

import com.library.pos.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
        List<Sale> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

        @org.springframework.data.jpa.repository.Query("SELECT s FROM Sale s WHERE s.timestamp BETWEEN :start AND :end "
                        +
                        "AND (:workerId IS NULL OR s.worker.id = :workerId) " +
                        "AND (:productName IS NULL OR LOWER(s.itemName) LIKE LOWER(CONCAT('%', :productName, '%')))")
        List<Sale> findByCriteria(@org.springframework.data.repository.query.Param("start") LocalDateTime start,
                        @org.springframework.data.repository.query.Param("end") LocalDateTime end,
                        @org.springframework.data.repository.query.Param("workerId") Long workerId,
                        @org.springframework.data.repository.query.Param("productName") String productName);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.worker.id = :workerId AND s.timestamp BETWEEN :start AND :end AND (s.status = 'SOLD' OR s.status = 'RETURNED')")
        Double calculateDailyCash(@org.springframework.data.repository.query.Param("workerId") Long workerId,
                        @org.springframework.data.repository.query.Param("start") LocalDateTime start,
                        @org.springframework.data.repository.query.Param("end") LocalDateTime end);

        @org.springframework.data.jpa.repository.Query("SELECT MAX(s.timestamp) FROM Sale s")
        LocalDateTime findLatestTimestamp();

        @org.springframework.data.jpa.repository.Query("SELECT SUM(ABS(s.totalAmount)) FROM Sale s WHERE s.status = 'RETURNED' AND s.timestamp BETWEEN :start AND :end")
        Double sumReturnsAmount(@org.springframework.data.repository.query.Param("start") LocalDateTime start,
                        @org.springframework.data.repository.query.Param("end") LocalDateTime end);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Sale s WHERE s.status = 'RETURNED' AND s.timestamp BETWEEN :start AND :end")
        Long countReturns(@org.springframework.data.repository.query.Param("start") LocalDateTime start,
                        @org.springframework.data.repository.query.Param("end") LocalDateTime end);

        @org.springframework.data.jpa.repository.Query("SELECT s.itemName, SUM(ABS(s.quantity)) as totalQty FROM Sale s WHERE s.status = 'RETURNED' AND s.timestamp BETWEEN :start AND :end GROUP BY s.itemName ORDER BY totalQty DESC")
        List<Object[]> findTopReturnedProducts(
                        @org.springframework.data.repository.query.Param("start") LocalDateTime start,
                        @org.springframework.data.repository.query.Param("end") LocalDateTime end,
                        org.springframework.data.domain.Pageable pageable);
}
