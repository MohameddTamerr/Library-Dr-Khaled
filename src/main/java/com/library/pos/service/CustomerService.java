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
        return customerRepository.findByCustomerNameContainingIgnoreCaseOrMobileContainingIgnoreCase(term, term);
    }

    public Customer save(Customer customer) {
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
}
