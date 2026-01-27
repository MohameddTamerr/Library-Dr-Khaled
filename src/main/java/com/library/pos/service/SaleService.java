package com.library.pos.service;

import com.library.pos.model.Product;
import com.library.pos.model.Sale;
import com.library.pos.model.SaleStatus;
import com.library.pos.repository.ProductRepository;
import com.library.pos.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    public SaleService(SaleRepository saleRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
    }

    public List<Sale> findByRange(LocalDateTime start, LocalDateTime end) {
        return saleRepository.findByTimestampBetween(start, end);
    }

    public List<Sale> getAll() {
        return saleRepository.findAll();
    }

    public List<Sale> search(LocalDateTime start, LocalDateTime end, Long workerId, String productName) {
        return saleRepository.findByCriteria(start, end, workerId, productName);
    }

    public Double getDailyCash(Long workerId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        Double total = saleRepository.calculateDailyCash(workerId, start, end);
        return total != null ? total : 0.0;
    }

    public Sale save(Sale sale) {
        Sale saved = saleRepository.save(sale);
        Product product = saved.getProduct();
        if (product != null) {
            int delta = saved.getQuantity() != null ? saved.getQuantity() : 0;
            if (saved.getStatus() == SaleStatus.RETURNED) {
                product.setQuantity(product.getQuantity() + delta);
            } else {
                product.setQuantity(product.getQuantity() - delta);
            }
            productRepository.save(product);
        }
        return saved;
    }

    public LocalDateTime getLatestSaleTimestamp() {
        return saleRepository.findLatestTimestamp();
    }

    public long getTotalCount() {
        return saleRepository.count();
    }

    public void saveSales(List<Sale> sales) {
        for (Sale sale : sales) {
            save(sale);
        }
    }
}
