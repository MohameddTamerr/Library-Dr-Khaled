package com.library.pos.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaFixer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("Checking database schema...");

            // Fix: Add withdrawal_limit to users table if missing
            try {
                jdbcTemplate.execute("SELECT withdrawal_limit FROM users LIMIT 1");
            } catch (Exception e) {
                System.out.println("Column 'withdrawal_limit' missing in 'users'. Adding it...");
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN withdrawal_limit DOUBLE DEFAULT 0.0");
                System.out.println("Column 'withdrawal_limit' added successfully.");
            }

            // Note: We don't drop 'address' column to avoid data loss, but the app ignores
            // it.

        } catch (Exception e) {
            System.err.println("Schema Fixer Error: " + e.getMessage());
            // Don't stop app, might be H2 or other issue
        }
    }
}
