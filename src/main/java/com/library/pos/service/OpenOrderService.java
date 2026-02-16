package com.library.pos.service;

import com.library.pos.model.OpenOrder;
import com.library.pos.model.OpenOrderStatus;
import com.library.pos.repository.OpenOrderItemRepository;
import com.library.pos.repository.OpenOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OpenOrderService {

    private final OpenOrderRepository openOrderRepository;
    private final OpenOrderItemRepository openOrderItemRepository;

    public OpenOrderService(OpenOrderRepository openOrderRepository, OpenOrderItemRepository openOrderItemRepository) {
        this.openOrderRepository = openOrderRepository;
        this.openOrderItemRepository = openOrderItemRepository;
    }

    @Transactional
    public List<OpenOrder> getOpenOrders() {
        // Clean corrupted rows at SQL level to avoid JPA nullability/cascade crashes.
        openOrderItemRepository.deleteItemsWithMissingProducts();
        openOrderRepository.deleteEmptyOpenOrdersByStatus(OpenOrderStatus.OPEN.name());

        List<OpenOrder> orders = openOrderRepository.findByStatusOrderByUpdatedAtDesc(OpenOrderStatus.OPEN);
        orders.forEach(order -> order.getItems().size());
        return orders;
    }

    public OpenOrder save(OpenOrder order) {
        return openOrderRepository.save(order);
    }

    public void delete(OpenOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        openOrderRepository.deleteById(order.getId());
    }
}
