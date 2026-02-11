package com.library.pos.service;

import com.library.pos.model.Category;
import com.library.pos.model.Product;
import com.library.pos.model.Supplier;
import com.library.pos.model.SupplierStatus;
import com.library.pos.repository.CategoryRepository;
import com.library.pos.repository.ProductRepository;
import com.library.pos.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, SupplierRepository supplierRepository,
            CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Product> getAll() {
        return productRepository.findAll();
    }

    public List<String> getAllCategories() {
        // Merge category names from both Categories table and Products table
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        // From dedicated categories table
        categoryRepository.findAll().stream()
                .map(Category::getName)
                .filter(n -> n != null && !n.isBlank())
                .forEach(names::add);
        // From existing products
        List<String> fromProducts = productRepository.findDistinctCategories();
        if (fromProducts != null) {
            fromProducts.stream().filter(s -> s != null && !s.isBlank()).forEach(names::add);
        }
        return List.copyOf(names);
    }

    public void saveCategory(String name) {
        if (name != null && !name.isBlank() && !categoryRepository.existsByNameIgnoreCase(name.trim())) {
            categoryRepository.save(new Category(name.trim()));
        }
    }

    public List<String> getAllSuppliers() {
        // Merge supplier names from both Products table and Suppliers table
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<String> fromProducts = productRepository.findDistinctSuppliers();
        if (fromProducts != null) {
            fromProducts.stream().filter(s -> s != null && !s.isBlank()).forEach(names::add);
        }
        // Add all active suppliers from the Suppliers table
        supplierRepository.findByStatusOrderByCreatedAtDesc(SupplierStatus.ACTIVE)
                .stream()
                .map(Supplier::getName)
                .filter(n -> n != null && !n.isBlank())
                .forEach(names::add);
        return List.copyOf(names);
    }

    public Optional<Product> findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }

    public List<Product> searchByBarcodeOrName(String term) {
        return productRepository.findByBarcodeContainingIgnoreCaseOrNameContainingIgnoreCase(term, term);
    }

    public Product save(Product product) {
        // Auto-persist category name to the categories table
        String cat = product.getCategory();
        if (cat != null && !cat.isBlank() && !categoryRepository.existsByNameIgnoreCase(cat.trim())) {
            categoryRepository.save(new Category(cat.trim()));
        }
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
