package com.library.pos.service;

import com.library.pos.model.Category;
import com.library.pos.model.Product;
import com.library.pos.model.ProductBarcode;
import com.library.pos.model.Supplier;
import com.library.pos.model.SupplierStatus;
import com.library.pos.repository.CategoryRepository;
import com.library.pos.repository.OpenOrderItemRepository;
import com.library.pos.repository.ProductBarcodeRepository;
import com.library.pos.repository.ProductRepository;
import com.library.pos.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductBarcodeRepository productBarcodeRepository;
    private final OpenOrderItemRepository openOrderItemRepository;
    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, ProductBarcodeRepository productBarcodeRepository,
            OpenOrderItemRepository openOrderItemRepository,
            SupplierRepository supplierRepository,
            CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.productBarcodeRepository = productBarcodeRepository;
        this.openOrderItemRepository = openOrderItemRepository;
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
        String normalized = normalizeBarcode(barcode);
        if (normalized == null) {
            return Optional.empty();
        }

        Optional<Product> direct = productRepository.findByBarcodeIgnoreCase(normalized);
        if (direct.isPresent()) {
            return direct;
        }

        return productBarcodeRepository.findByBarcodeIgnoreCase(normalized).map(ProductBarcode::getProduct);
    }

    public List<Product> searchByBarcodeOrName(String term) {
        if (term == null || term.isBlank()) {
            return List.of();
        }
        return productRepository.searchByAnyBarcodeOrName(term.trim());
    }

    public Product save(Product product) {
        return save(product, List.of());
    }

    @Transactional
    public Product save(Product product, List<String> additionalBarcodes) {
        String primaryBarcode = normalizeBarcode(product != null ? product.getBarcode() : null);
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        if (primaryBarcode == null) {
            throw new IllegalArgumentException("Primary barcode is required.");
        }

        Long currentProductId = product.getId();
        if (isBarcodeUsedByAnotherProduct(primaryBarcode, currentProductId)) {
            throw new IllegalArgumentException("Barcode already exists: " + primaryBarcode);
        }

        Set<String> normalizedAdditional = normalizeAdditionalBarcodes(additionalBarcodes, primaryBarcode);
        for (String barcode : normalizedAdditional) {
            if (isBarcodeUsedByAnotherProduct(barcode, currentProductId)) {
                throw new IllegalArgumentException("Barcode already exists: " + barcode);
            }
        }

        Product target;
        if (currentProductId != null) {
            target = productRepository.findById(currentProductId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + currentProductId));
        } else {
            target = product;
        }

        target.setName(product.getName());
        target.setBarcode(primaryBarcode);
        target.setCategory(product.getCategory());
        target.setSupplier(product.getSupplier());
        target.setCost(product.getCost());
        target.setSellPrice(product.getSellPrice());
        target.setQuantity(product.getQuantity());
        target.setMinStock(product.getMinStock());

        // Auto-persist category name to the categories table
        String cat = target.getCategory();
        if (cat != null && !cat.isBlank() && !categoryRepository.existsByNameIgnoreCase(cat.trim())) {
            categoryRepository.save(new Category(cat.trim()));
        }

        if (target.getAdditionalBarcodes() == null) {
            target.setAdditionalBarcodes(new ArrayList<>());
        } else {
            target.getAdditionalBarcodes().clear();
        }

        for (String barcode : normalizedAdditional) {
            target.getAdditionalBarcodes().add(new ProductBarcode(target, barcode));
        }

        return productRepository.save(target);
    }

    public List<String> getAdditionalBarcodes(Long productId) {
        if (productId == null) {
            return List.of();
        }
        return productBarcodeRepository.findByProductIdOrderByIdAsc(productId)
                .stream()
                .map(ProductBarcode::getBarcode)
                .filter(s -> s != null && !s.isBlank())
                .toList();
    }

    public boolean isBarcodeUsedByAnotherProduct(String barcode, Long currentProductId) {
        String normalized = normalizeBarcode(barcode);
        if (normalized == null) {
            return false;
        }

        Optional<Product> direct = productRepository.findByBarcodeIgnoreCase(normalized);
        if (direct.isPresent() && !isSameProduct(direct.get().getId(), currentProductId)) {
            return true;
        }

        Optional<ProductBarcode> additional = productBarcodeRepository.findByBarcodeIgnoreCase(normalized);
        if (additional.isPresent()) {
            Product owner = additional.get().getProduct();
            Long ownerId = owner != null ? owner.getId() : null;
            if (!isSameProduct(ownerId, currentProductId)) {
                return true;
            }
        }

        return false;
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }
        // Prevent orphaned references in open orders when a product is deleted.
        openOrderItemRepository.deleteByProductId(id);
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

    private Set<String> normalizeAdditionalBarcodes(List<String> barcodes, String primaryBarcode) {
        if (barcodes == null || barcodes.isEmpty()) {
            return Set.of();
        }

        Set<String> deduped = new LinkedHashSet<>();
        Set<String> lowerSeen = new LinkedHashSet<>();
        String primaryLower = primaryBarcode == null ? "" : primaryBarcode.toLowerCase(Locale.ROOT);

        for (String barcode : barcodes) {
            String normalized = normalizeBarcode(barcode);
            if (normalized == null) {
                continue;
            }
            String lower = normalized.toLowerCase(Locale.ROOT);
            if (lower.equals(primaryLower)) {
                continue;
            }
            if (lowerSeen.add(lower)) {
                deduped.add(normalized);
            }
        }

        return deduped;
    }

    private String normalizeBarcode(String barcode) {
        if (barcode == null) {
            return null;
        }
        String value = barcode.trim();
        if (value.isBlank()) {
            return null;
        }
        return value;
    }

    private boolean isSameProduct(Long first, Long second) {
        if (first == null || second == null) {
            return false;
        }
        return first.equals(second);
    }
}
