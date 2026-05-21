package com.library.pos.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.library.pos.model.WastedItem;

public interface WastedItemRepository extends JpaRepository<WastedItem, Long> {

    List<WastedItem> findAllByOrderByWastedAtDesc();

    List<WastedItem> findByWastedAtBetweenOrderByWastedAtDesc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(w.totalCostLoss), 0) FROM WastedItem w WHERE w.wastedAt BETWEEN :start AND :end")
    Double sumCostLossBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
