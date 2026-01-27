package com.library.pos.service;

import com.library.pos.model.Product;
import com.library.pos.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAll() {
        return productRepository.findAll();
    }

    public List<String> getAllCategories() {
        return productRepository.findDistinctCategories();
    }

    public Optional<Product> findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }

    public List<Product> searchByBarcodeOrName(String term) {
        return productRepository.findByBarcodeContainingIgnoreCaseOrNameContainingIgnoreCase(term, term);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public void deleteById(Long id) {
        productRepository.deleteById(id);

    }

    public java.time.LocalDateTime getLatestUpdateTime() {
        return productRepository.findLatestUpdate();
    }

    public long getTotalCount() {
        return productRepository.count();
    }

    public void updateStock(Long productId, int quantityChange) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            int newQty = product.getQuantity() + quantityChange;
            product.setQuantity(newQty);
            productRepository.save(product);
        }
    }
}
