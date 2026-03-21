package com.library.pos.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.pos.model.Product;
import com.library.pos.model.User;
import com.library.pos.model.WastedItem;
import com.library.pos.repository.ProductRepository;
import com.library.pos.repository.WastedItemRepository;

@Service
public class WastedItemService {

    private final WastedItemRepository wastedItemRepository;
    private final ProductRepository productRepository;

    public WastedItemService(WastedItemRepository wastedItemRepository, ProductRepository productRepository) {
        this.wastedItemRepository = wastedItemRepository;
        this.productRepository = productRepository;
    }

    /**
     * Record a wasted item: subtracts quantity from stock and saves the record.
     */
    @Transactional
    public WastedItem recordWaste(Product product, int quantity, User worker, String notes) {
        if (product == null || quantity <= 0) {
            throw new IllegalArgumentException("مشكلة في بيانات الهالك");
        }
        if (product.getQuantity() < quantity) {
            throw new IllegalStateException(
                    "الكمية المطلوبة (" + quantity + ") أكبر من المخزون المتاح (" + product.getQuantity() + ")");
        }

        // Subtract from stock
        product.setQuantity(product.getQuantity() - quantity);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        // Save wasted record
        WastedItem wasted = new WastedItem(product, quantity, worker, notes);
        return wastedItemRepository.save(wasted);
    }

    public List<WastedItem> getAll() {
        return wastedItemRepository.findAllByOrderByWastedAtDesc();
    }

    public List<WastedItem> getByRange(LocalDateTime start, LocalDateTime end) {
        return wastedItemRepository.findByWastedAtBetweenOrderByWastedAtDesc(start, end);
    }

    /**
     * Total cost loss from wasted items in the given period.
     * This value should be subtracted from net profit.
     */
    public double getTotalCostLoss(LocalDateTime start, LocalDateTime end) {
        Double val = wastedItemRepository.sumCostLossBetween(start, end);
        return val != null ? val : 0.0;
    }
}
