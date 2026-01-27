package com.library.pos.repository;

import com.library.pos.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByCustomerNameContainingIgnoreCaseOrMobileContainingIgnoreCase(String name, String mobile);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(c.updatedAt) FROM Customer c")
    java.time.LocalDateTime findLatestUpdate();
}
