package com.library.pos.repository;

import com.library.pos.model.OpenOrder;
import com.library.pos.model.OpenOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OpenOrderRepository extends JpaRepository<OpenOrder, Long> {
    List<OpenOrder> findByStatusOrderByUpdatedAtDesc(OpenOrderStatus status);

    @Modifying
    @Query(value = "delete from open_orders where status = :status and open_order_id not in (select distinct open_order_id from open_order_items)", nativeQuery = true)
    int deleteEmptyOpenOrdersByStatus(@Param("status") String status);
}
