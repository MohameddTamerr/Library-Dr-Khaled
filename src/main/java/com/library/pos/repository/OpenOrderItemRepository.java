package com.library.pos.repository;

import com.library.pos.model.OpenOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpenOrderItemRepository extends JpaRepository<OpenOrderItem, Long> {

    @Modifying
    @Query("delete from OpenOrderItem i where i.product.id = :productId")
    int deleteByProductId(@Param("productId") Long productId);

    @Modifying
    @Query(value = "delete from open_order_items where product_id not in (select product_id from products)", nativeQuery = true)
    int deleteItemsWithMissingProducts();
}
