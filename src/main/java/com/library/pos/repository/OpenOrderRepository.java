package com.library.pos.repository;

import com.library.pos.model.OpenOrder;
import com.library.pos.model.OpenOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpenOrderRepository extends JpaRepository<OpenOrder, Long> {
    List<OpenOrder> findByStatusOrderByUpdatedAtDesc(OpenOrderStatus status);
}
