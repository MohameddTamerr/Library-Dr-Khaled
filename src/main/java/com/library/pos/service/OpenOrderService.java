package com.library.pos.service;

import com.library.pos.model.OpenOrder;
import com.library.pos.model.OpenOrderStatus;
import com.library.pos.repository.OpenOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OpenOrderService {

    private final OpenOrderRepository openOrderRepository;

    public OpenOrderService(OpenOrderRepository openOrderRepository) {
        this.openOrderRepository = openOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<OpenOrder> getOpenOrders() {
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
