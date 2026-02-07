package com.library.pos.repository;

import com.library.pos.model.Supplier;
import com.library.pos.model.SupplierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByStatusOrderByCreatedAtDesc(SupplierStatus status);

    List<Supplier> findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(String name, String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Supplier s where s.id = :id")
    Supplier findByIdForUpdate(@Param("id") Long id);

    boolean existsByNameAndPhone(String name, String phone);
}
