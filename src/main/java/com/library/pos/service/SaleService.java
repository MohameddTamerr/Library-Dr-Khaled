package com.library.pos.service;

import com.library.pos.model.Product;
import com.library.pos.model.Sale;
import com.library.pos.model.SaleStatus;
import com.library.pos.repository.ProductRepository;
import com.library.pos.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<Sale> getDeferredWithSavedCustomers(String term) {
        String normalized = term;
        if (normalized != null && normalized.isBlank()) {
            normalized = null;
        }
        return saleRepository.findDeferredWithSavedCustomers(SaleStatus.DEFERRED, normalized);
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

    public Double getReturnsAmount(LocalDateTime start, LocalDateTime end) {
        Double val = saleRepository.sumReturnsAmount(start, end);
        return val != null ? val : 0.0;
    }

    public Long getReturnsCount(LocalDateTime start, LocalDateTime end) {
        Long val = saleRepository.countReturns(start, end);
        return val != null ? val : 0L;
    }

    public List<Object[]> getTopReturnedProducts(LocalDateTime start, LocalDateTime end) {
        return saleRepository.findTopReturnedProducts(start, end, org.springframework.data.domain.PageRequest.of(0, 5));
    }

    @Transactional
    public List<Sale> saveSales(List<Sale> sales) {
        List<Sale> savedSales = new java.util.ArrayList<>();
        if (sales == null) {
            return savedSales;
        }
        for (Sale sale : sales) {
            if (sale == null) {
                continue;
            }
            savedSales.add(save(sale));
        }
        return savedSales;
    }

    public double calculateNetProfit(List<Sale> sales) {
        if (sales == null || sales.isEmpty()) {
            return 0.0;
        }
        double netProfit = 0.0;
        for (Sale sale : sales) {
            if (sale == null || sale.getStatus() == null) {
                continue;
            }
            int quantityAbs = Math.abs(sale.getQuantity() != null ? sale.getQuantity() : 0);
            if (quantityAbs == 0) {
                continue;
            }

            double totalAmountAbs = Math.abs(sale.getTotalAmount() != null ? sale.getTotalAmount() : 0.0);
            double unitSellPrice = totalAmountAbs / quantityAbs;
            Double productCost = sale.getProduct() != null ? sale.getProduct().getCost() : null;
            // Fallback when cost is unavailable in historical row.
            double unitCost = productCost != null ? productCost : (unitSellPrice * 0.75);
            double unitMargin = unitSellPrice - unitCost;

            switch (sale.getStatus()) {
                case SOLD, DEFERRED, DELIVERY -> netProfit += unitMargin * quantityAbs;
                case RETURNED -> {
                    boolean badCondition = sale.getReturnCondition() != null
                            && "BAD".equalsIgnoreCase(sale.getReturnCondition());
                    if (badCondition) {
                        // Damaged return: full refunded sale value is a loss.
                        netProfit -= totalAmountAbs;
                    } else {
                        // Good return: reverse only the earned margin.
                        netProfit -= unitMargin * quantityAbs;
                    }
                }
            }
        }
        return netProfit;
    }

    /**
     * Process a product return
     * 
     * @param originalSale     The original sale to return from
     * @param quantityToReturn The quantity being returned (positive number)
     * @param condition        "GOOD" or "BAD" - determines if inventory is restored
     * @param returnWorker     The worker processing the return
     * @return The created return Sale record
     */
    public Sale processReturn(Sale originalSale, int quantityToReturn, String condition,
            com.library.pos.model.User returnWorker) {
        if (originalSale == null || quantityToReturn <= 0) {
            throw new IllegalArgumentException("Invalid return parameters");
        }

        // Calculate return amount based on original price
        double unitPrice = originalSale.getTotalAmount() / originalSale.getQuantity();
        double returnAmount = unitPrice * quantityToReturn;

        // Create a new Sale record for the return
        Sale returnSale = new Sale();
        returnSale.setTimestamp(LocalDateTime.now());
        returnSale.setItemName(originalSale.getItemName());
        returnSale.setQuantity(-quantityToReturn); // Negative quantity
        returnSale.setTotalAmount(-returnAmount); // Negative amount (refund)
        returnSale.setStatus(SaleStatus.RETURNED);
        returnSale.setWorker(returnWorker);
        returnSale.setCustomer(originalSale.getCustomer());
        returnSale.setProduct(originalSale.getProduct());
        returnSale.setReturnCondition(condition);
        returnSale.setNotes("Return from Order #" + originalSale.getId());

        // Save the return record
        Sale savedReturn = saleRepository.save(returnSale);

        // Restore inventory only if condition is GOOD
        if ("GOOD".equalsIgnoreCase(condition)) {
            Product product = originalSale.getProduct();
            if (product != null) {
                product.setQuantity(product.getQuantity() + quantityToReturn);
                productRepository.save(product);
            }
        }

        return savedReturn;
    }

    /**
     * Process a product return (simplified - no original sale required)
     * 
     * @param product          The product being returned
     * @param quantityToReturn The quantity being returned (positive number)
     * @param condition        "GOOD" or "BAD" - determines if inventory is restored
     * @param returnWorker     The worker processing the return
     * @return The created return Sale record
     */
    @Transactional
    public void deleteAllSales() {
        saleRepository.deleteAll();
    }

    public Sale processProductReturn(Product product, int quantityToReturn, String condition,
            com.library.pos.model.User returnWorker) {
        if (product == null || quantityToReturn <= 0) {
            throw new IllegalArgumentException("Invalid return parameters");
        }

        // Calculate return amount based on product sell price
        double returnAmount = product.getSellPrice() * quantityToReturn;

        // Create a new Sale record for the return
        Sale returnSale = new Sale();
        returnSale.setTimestamp(LocalDateTime.now());
        returnSale.setItemName(product.getName());
        returnSale.setQuantity(-quantityToReturn); // Negative quantity
        returnSale.setTotalAmount(-returnAmount); // Negative amount (refund)
        returnSale.setStatus(SaleStatus.RETURNED);
        returnSale.setWorker(returnWorker);
        returnSale.setProduct(product);
        returnSale.setReturnCondition(condition);
        returnSale.setNotes("Product Return - " + condition + " condition");

        // Save the return record
        Sale savedReturn = saleRepository.save(returnSale);

        // Restore inventory only if condition is GOOD
        if ("GOOD".equalsIgnoreCase(condition)) {
            product.setQuantity(product.getQuantity() + quantityToReturn);
            productRepository.save(product);
        }

        return savedReturn;
    }
}
