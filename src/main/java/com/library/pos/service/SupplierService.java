package com.library.pos.service;

import com.library.pos.model.PaymentMethod;
import com.library.pos.model.PaymentStatus;
import com.library.pos.model.Purchase;
import com.library.pos.model.Supplier;
import com.library.pos.model.SupplierPayment;
import com.library.pos.model.SupplierStatus;
import com.library.pos.repository.PurchaseRepository;
import com.library.pos.repository.SupplierPaymentRepository;
import com.library.pos.repository.SupplierRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final SupplierPaymentRepository paymentRepository;

    public SupplierService(SupplierRepository supplierRepository,
            PurchaseRepository purchaseRepository,
            SupplierPaymentRepository paymentRepository) {
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.paymentRepository = paymentRepository;
    }

    public Supplier saveSupplier(Supplier supplier) {
        validateSupplier(supplier);
        if (supplier.getId() == null) {
            if (supplierRepository.existsByNameAndPhone(supplier.getName(), supplier.getPhone())) {
                throw new IllegalArgumentException("Supplier with this name and phone already exists.");
            }
            applySupplierDefaults(supplier);
            applyBalancesForCreate(supplier);
            return supplierRepository.save(supplier);
        }
        Supplier existing = supplierRepository.findById(supplier.getId())
                .orElseThrow(() -> new IllegalStateException("Supplier not found for update."));
        existing.setName(supplier.getName());
        existing.setPhone(supplier.getPhone());
        existing.setEmail(supplier.getEmail());
        existing.setAddress(supplier.getAddress());
        existing.setStatus(statusOrDefault(supplier.getStatus()));
        if (supplier.getTotalPurchases() != null) {
            BigDecimal totalPurchases = valueOrZero(supplier.getTotalPurchases());
            if (totalPurchases.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Total purchases cannot be negative.");
            }
            BigDecimal totalPaid = valueOrZero(existing.getTotalPaid());
            if (totalPurchases.compareTo(totalPaid) < 0) {
                throw new IllegalArgumentException("Total purchases cannot be أقل من إجمالي المدفوع.");
            }
            existing.setTotalPurchases(totalPurchases);
            existing.setBalanceDue(totalPurchases.subtract(totalPaid));
        }
        return supplierRepository.save(existing);
    }

    public void updateStatus(long supplierId, SupplierStatus status) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalStateException("Supplier not found for status update."));
        supplier.setStatus(statusOrDefault(status));
        supplierRepository.save(supplier);
    }

    public Optional<Supplier> findById(long supplierId) {
        return supplierRepository.findById(supplierId);
    }

    public List<Supplier> listAll(boolean activeOnly) {
        if (activeOnly) {
            return supplierRepository.findByStatusOrderByCreatedAtDesc(SupplierStatus.ACTIVE);
        }
        return supplierRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<Supplier> searchByNameOrPhone(String term) {
        return supplierRepository.findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(term, term);
    }

    @Transactional
    public Purchase recordPurchase(Purchase purchase) {
        validatePurchase(purchase);
        Supplier supplier = resolveSupplierForPurchase(purchase);
        purchase.setSupplier(supplier);
        purchase.setPaymentStatus(resolvePaymentStatus(purchase.getTotalAmount(), purchase.getPaidAmount()));
        if (purchase.getPurchaseDate() == null) {
            purchase.setPurchaseDate(LocalDateTime.now());
        }

        Supplier locked = supplierRepository.findByIdForUpdate(supplier.getId());
        BigDecimal newTotalPurchases = valueOrZero(locked.getTotalPurchases())
                .add(valueOrZero(purchase.getTotalAmount()));
        BigDecimal newTotalPaid = valueOrZero(locked.getTotalPaid())
                .add(valueOrZero(purchase.getPaidAmount()));
        BigDecimal newBalance = newTotalPurchases.subtract(newTotalPaid);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance would be negative after this purchase.");
        }
        locked.setTotalPurchases(newTotalPurchases);
        locked.setTotalPaid(newTotalPaid);
        locked.setBalanceDue(newBalance);

        Purchase saved = purchaseRepository.save(purchase);
        supplierRepository.save(locked);
        return saved;
    }

    @Transactional
    public SupplierPayment recordPayment(SupplierPayment payment) {
        validatePayment(payment);
        Supplier supplier = resolveSupplierForPayment(payment);
        payment.setSupplier(supplier);
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }
        if (payment.getMethod() == null) {
            payment.setMethod(PaymentMethod.CASH);
        }
        if (payment.getMethodDisplay() == null || payment.getMethodDisplay().isBlank()) {
            payment.setMethodDisplay(mapMethodDisplay(payment.getMethod()));
        }

        Supplier locked = supplierRepository.findByIdForUpdate(supplier.getId());
        BigDecimal newTotalPaid = valueOrZero(locked.getTotalPaid())
                .add(valueOrZero(payment.getAmount()));
        BigDecimal newBalance = valueOrZero(locked.getTotalPurchases()).subtract(newTotalPaid);
        // if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
        // throw new IllegalArgumentException("Payment exceeds supplier balance due.");
        // }
        locked.setTotalPaid(newTotalPaid);
        locked.setBalanceDue(newBalance);

        SupplierPayment saved = paymentRepository.save(payment);
        supplierRepository.save(locked);
        return saved;
    }

    public List<Purchase> listPurchases(long supplierId) {
        return purchaseRepository.findBySupplier_IdOrderByPurchaseDateDesc(supplierId);
    }

    public List<SupplierPayment> listPayments(long supplierId) {
        return paymentRepository.findBySupplier_IdOrderByPaymentDateDesc(supplierId);
    }

    public BigDecimal getDailyPaymentsTotal() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        BigDecimal total = paymentRepository.calculateDailyTotal(start, end);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getDailyCashPaymentsTotal() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        BigDecimal total = paymentRepository.calculateDailyCashTotal(start, end);
        return total != null ? total : BigDecimal.ZERO;
    }

    private void validateSupplier(Supplier supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier data is required.");
        }
        if (supplier.getName() == null || supplier.getName().isBlank()) {
            throw new IllegalArgumentException("Supplier name is required.");
        }
    }

    private void validatePurchase(Purchase purchase) {
        if (purchase == null) {
            throw new IllegalArgumentException("Purchase data is required.");
        }
        Supplier supplier = purchase.getSupplier();
        if (supplier == null || supplier.getId() == null) {
            throw new IllegalArgumentException("Supplier is required for purchase.");
        }
        BigDecimal total = valueOrZero(purchase.getTotalAmount());
        BigDecimal paid = valueOrZero(purchase.getPaidAmount());
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero.");
        }
        if (paid.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Paid amount cannot be negative.");
        }
        if (paid.compareTo(total) > 0) {
            throw new IllegalArgumentException("Paid amount cannot exceed total amount.");
        }
    }

    private void validatePayment(SupplierPayment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment data is required.");
        }
        Supplier supplier = payment.getSupplier();
        if (supplier == null || supplier.getId() == null) {
            throw new IllegalArgumentException("Supplier is required for payment.");
        }
        BigDecimal amount = valueOrZero(payment.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        Supplier existing = supplierRepository.findById(supplier.getId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found."));
        // BigDecimal balance = valueOrZero(existing.getBalanceDue());
        // if (amount.compareTo(balance) > 0) {
        // throw new IllegalArgumentException("Payment exceeds supplier balance due.");
        // }
    }

    private Supplier resolveSupplierForPurchase(Purchase purchase) {
        Supplier supplier = purchase.getSupplier();
        return supplierRepository.findById(supplier.getId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found."));
    }

    private Supplier resolveSupplierForPayment(SupplierPayment payment) {
        Supplier supplier = payment.getSupplier();
        return supplierRepository.findById(supplier.getId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found."));
    }

    private PaymentStatus resolvePaymentStatus(BigDecimal total, BigDecimal paid) {
        BigDecimal totalAmount = valueOrZero(total);
        BigDecimal paidAmount = valueOrZero(paid);
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentStatus.UNPAID;
        }
        if (paidAmount.compareTo(totalAmount) < 0) {
            return PaymentStatus.PARTIAL;
        }
        return PaymentStatus.PAID;
    }

    private void applySupplierDefaults(Supplier supplier) {
        supplier.setStatus(statusOrDefault(supplier.getStatus()));
        supplier.setTotalPurchases(valueOrZero(supplier.getTotalPurchases()));
        supplier.setTotalPaid(valueOrZero(supplier.getTotalPaid()));
        supplier.setBalanceDue(valueOrZero(supplier.getBalanceDue()));
    }

    private void applyBalancesForCreate(Supplier supplier) {
        BigDecimal totalPurchases = valueOrZero(supplier.getTotalPurchases());
        if (totalPurchases.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total purchases cannot be negative.");
        }
        supplier.setTotalPaid(BigDecimal.ZERO);
        supplier.setBalanceDue(totalPurchases);
    }

    private SupplierStatus statusOrDefault(SupplierStatus status) {
        return status == null ? SupplierStatus.ACTIVE : status;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String mapMethodDisplay(PaymentMethod method) {
        if (method == null) {
            return "Cash";
        }
        return switch (method) {
            case CASH -> "Cash";
            case BANK -> "Bank";
            default -> "Other";
        };
    }

    @Transactional
    public void mergeDuplicates() {
        List<Supplier> allSuppliers = supplierRepository.findAll();
        // Group by Name + Phone (normalized)
        java.util.Map<String, List<Supplier>> groups = allSuppliers.stream()
                .collect(java.util.stream.Collectors
                        .groupingBy(s -> (s.getName() != null ? s.getName().trim().toLowerCase() : "") + "|" +
                                (s.getPhone() != null ? s.getPhone().trim() : "")));

        for (List<Supplier> group : groups.values()) {
            if (group.size() > 1) {
                // Keep the one with the most activity or the oldest
                // Sort by ID (oldest first)
                group.sort(java.util.Comparator.comparing(Supplier::getId));

                Supplier primary = group.get(0);
                List<Supplier> duplicates = group.subList(1, group.size());

                for (Supplier duplicate : duplicates) {
                    // Move Purchases
                    List<Purchase> purchases = purchaseRepository
                            .findBySupplier_IdOrderByPurchaseDateDesc(duplicate.getId());
                    for (Purchase p : purchases) {
                        p.setSupplier(primary);
                        purchaseRepository.save(p);
                    }

                    // Move Payments
                    List<SupplierPayment> payments = paymentRepository
                            .findBySupplier_IdOrderByPaymentDateDesc(duplicate.getId());
                    for (SupplierPayment sp : payments) {
                        sp.setSupplier(primary);
                        paymentRepository.save(sp);
                    }

                    // Merge Totals
                    primary.setTotalPurchases(
                            valueOrZero(primary.getTotalPurchases()).add(valueOrZero(duplicate.getTotalPurchases())));
                    primary.setTotalPaid(
                            valueOrZero(primary.getTotalPaid()).add(valueOrZero(duplicate.getTotalPaid())));
                    primary.setBalanceDue(
                            valueOrZero(primary.getBalanceDue()).add(valueOrZero(duplicate.getBalanceDue())));

                    // Update Status if duplicate was active
                    if (duplicate.getStatus() == SupplierStatus.ACTIVE) {
                        primary.setStatus(SupplierStatus.ACTIVE);
                    }

                    // Delete duplicate
                    supplierRepository.delete(duplicate);
                }
                supplierRepository.save(primary);
            }
        }
    }
}
