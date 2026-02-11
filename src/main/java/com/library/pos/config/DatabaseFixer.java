package com.library.pos.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class DatabaseFixer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("Running Database Schema Fixes...");
            // Fix status column length/type for 'DELIVERY' support
            try {
                jdbcTemplate.execute("ALTER TABLE orders MODIFY COLUMN status VARCHAR(50)");
                System.out.println("Schema fix executed: ALTER TABLE orders MODIFY COLUMN status VARCHAR(50)");
            } catch (Exception e) {
                // Ignore if SQLite (feature not supported/syntax different) or already applied
                // SQLite doesn't support MODIFY COLUMN directly
                if (!e.getMessage().contains("SQLITE_ERROR")) {
                    System.out.println("Schema fix warning (orders status): " + e.getMessage());
                }
            }

            try {
                jdbcTemplate.execute("ALTER TABLE customers ADD COLUMN customer_code VARCHAR(5)");
                System.out.println("Schema fix executed: ALTER TABLE customers ADD COLUMN customer_code VARCHAR(5)");
            } catch (Exception ex) {
                System.out.println("Schema fix warning (customer_code column): " + ex.getMessage());
            }

            try {
                jdbcTemplate.execute("CREATE UNIQUE INDEX uk_customers_customer_code ON customers (customer_code)");
                System.out.println("Schema fix executed: CREATE UNIQUE INDEX uk_customers_customer_code");
            } catch (Exception ex) {
                System.out.println("Schema fix warning (customer_code index): " + ex.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Schema fix warning (might be already fixed or table missing): " + e.getMessage());
        }
    }
}
