package com.library.pos.service;

import com.library.pos.model.Customer;
import com.library.pos.model.CustomerDeferredPayment;
import com.library.pos.model.CustomerDeferredSummary;
import com.library.pos.model.PaymentMethod;
import com.library.pos.model.PaymentStatus;
import com.library.pos.model.Sale;
import com.library.pos.model.SaleStatus;
import com.library.pos.repository.CustomerDeferredPaymentRepository;
import com.library.pos.repository.CustomerRepository;
import com.library.pos.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerDeferredService {

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDeferredPaymentRepository paymentRepository;

    public CustomerDeferredService(SaleRepository saleRepository, CustomerRepository customerRepository,
            CustomerDeferredPaymentRepository paymentRepository) {
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<CustomerDeferredSummary> listCustomerSummaries(String term) {
        String normalized = term;
        if (normalized != null && normalized.isBlank()) {
            normalized = null;
        }
        List<Sale> deferredSales = saleRepository.findDeferredWithSavedCustomers(SaleStatus.DEFERRED, normalized);
        Map<Long, Customer> customerById = new LinkedHashMap<>();
        Map<Long, BigDecimal> deferredTotals = new LinkedHashMap<>();

        for (Sale sale : deferredSales) {
            Customer customer = sale.getCustomer();
            if (customer == null || customer.getId() == null) {
                continue;
            }
            Long customerId = customer.getId();
            customerById.putIfAbsent(customerId, customer);
            BigDecimal current = deferredTotals.getOrDefault(customerId, BigDecimal.ZERO);
            BigDecimal amount = BigDecimal.valueOf(sale.getTotalAmount() != null ? sale.getTotalAmount() : 0.0);
            deferredTotals.put(customerId, current.add(amount));
        }

        List<Long> customerIds = new ArrayList<>(customerById.keySet());
        Map<Long, BigDecimal> paidTotals = new LinkedHashMap<>();
        if (!customerIds.isEmpty()) {
            List<Object[]> rows = paymentRepository.sumAmountsByCustomerIds(customerIds);
            for (Object[] row : rows) {
                Long customerId = (Long) row[0];
                BigDecimal amount = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                paidTotals.put(customerId, amount);
            }
        }

        List<CustomerDeferredSummary> results = new ArrayList<>();
        for (Long customerId : customerIds) {
            Customer customer = customerById.get(customerId);
            BigDecimal totalDeferred = deferredTotals.getOrDefault(customerId, BigDecimal.ZERO);
            BigDecimal totalPaid = paidTotals.getOrDefault(customerId, BigDecimal.ZERO);
            BigDecimal balance = totalDeferred.subtract(totalPaid);
            PaymentStatus status = resolveStatus(totalDeferred, totalPaid, balance);
            results.add(new CustomerDeferredSummary(customer, totalDeferred, totalPaid, balance, status));
        }

        results.sort(Comparator.comparing(CustomerDeferredSummary::getBalanceDue).reversed()
                .thenComparing(s -> s.getCustomer() != null ? s.getCustomer().getCustomerName() : ""));
        return results;
    }

    public List<CustomerDeferredPayment> listPayments(Long customerId) {
        if (customerId == null) {
            return java.util.Collections.emptyList();
        }
        return paymentRepository.findByCustomer_IdOrderByPaymentDateDesc(customerId);
    }

    @Transactional
    public CustomerDeferredPayment recordPayment(CustomerDeferredPayment payment) {
        validatePayment(payment);
        Customer customer = customerRepository.findById(payment.getCustomer().getId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found."));
        payment.setCustomer(customer);

        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }
        if (payment.getMethod() == null) {
            payment.setMethod(PaymentMethod.CASH);
        }
        return paymentRepository.save(payment);
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

    public BigDecimal getBalanceDueForCustomer(Long customerId) {
        BigDecimal balance = getBalanceDue(customerId);
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return balance;
    }

    private void validatePayment(CustomerDeferredPayment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment data is required.");
        }
        Customer customer = payment.getCustomer();
        if (customer == null || customer.getId() == null) {
            throw new IllegalArgumentException("Customer is required for payment.");
        }
        BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        BigDecimal balanceDue = getBalanceDue(customer.getId());
        if (balanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("لا يوجد رصيد آجل مستحق لهذا العميل. المتبقي: 0.00 ج.م");
        }
        if (amount.compareTo(balanceDue) > 0) {
            String remaining = balanceDue.setScale(2, RoundingMode.HALF_UP).toPlainString();
            throw new IllegalArgumentException("لا يمكن دفع مبلغ أكبر من المتبقي. المتبقي: "
                    + remaining + " ج.م");
        }
    }

    private BigDecimal getBalanceDue(Long customerId) {
        if (customerId == null) {
            return BigDecimal.ZERO;
        }
        Double deferredTotalRaw = saleRepository.sumTotalAmountByStatusAndCustomerId(SaleStatus.DEFERRED, customerId);
        BigDecimal deferredTotal = BigDecimal.valueOf(deferredTotalRaw != null ? deferredTotalRaw : 0.0);
        BigDecimal paidTotal = paymentRepository.sumByCustomerId(customerId);
        if (paidTotal == null) {
            paidTotal = BigDecimal.ZERO;
        }
        return deferredTotal.subtract(paidTotal);
    }

    private PaymentStatus resolveStatus(BigDecimal totalDeferred, BigDecimal totalPaid, BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) <= 0 && totalDeferred.compareTo(BigDecimal.ZERO) > 0) {
            return PaymentStatus.PAID;
        }
        if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            return PaymentStatus.PARTIAL;
        }
        return PaymentStatus.UNPAID;
    }
}
