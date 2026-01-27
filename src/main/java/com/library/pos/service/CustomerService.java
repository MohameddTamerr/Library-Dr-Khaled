package com.library.pos.service;

import com.library.pos.model.Customer;
import com.library.pos.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    public List<Customer> search(String term) {
        return customerRepository
                .findByCustomerNameContainingIgnoreCaseOrMobileContainingIgnoreCaseOrCustomerCodeContainingIgnoreCase(
                        term, term, term);
    }

    public Customer save(Customer customer) {
        ensureCustomerCode(customer);
        return customerRepository.save(customer);
    }

    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }

    public java.time.LocalDateTime getLatestUpdateTime() {
        return customerRepository.findLatestUpdate();
    }

    public long getTotalCount() {
        return customerRepository.count();
    }

    public List<Customer> searchCustomers(String term) {
        return search(term);
    }

    public Customer saveCustomer(Customer customer) {
        return save(customer);
    }

    private void ensureCustomerCode(Customer customer) {
        if (customer == null) {
            return;
        }
        String code = customer.getCustomerCode();
        if (code != null && !code.isBlank()) {
            return;
        }
        long count = customerRepository.count();
        boolean useFiveDigits = count >= 10000;
        String generated = generateUniqueCode(useFiveDigits ? 5 : 4);
        if (generated == null && !useFiveDigits) {
            generated = generateUniqueCode(5);
        }
        if (generated != null) {
            customer.setCustomerCode(generated);
        }
    }

    private String generateUniqueCode(int digits) {
        if (digits < 4 || digits > 5) {
            digits = 4;
        }
        int maxAttempts = 200;
        int min = (int) Math.pow(10, digits - 1);
        int max = (int) Math.pow(10, digits) - 1;
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < maxAttempts; i++) {
            int value = min + random.nextInt(max - min + 1);
            String code = String.valueOf(value);
            if (!customerRepository.existsByCustomerCode(code)) {
                return code;
            }
        }
        return null;
    }
}
