package com.library.pos.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

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

            try {
                jdbcTemplate.execute(
                        "ALTER TABLE supplier_payments MODIFY COLUMN method VARCHAR(20) NOT NULL DEFAULT 'CASH'");
                System.out.println("Schema fix executed: ALTER TABLE supplier_payments MODIFY COLUMN method VARCHAR(20)");
            } catch (Exception ex) {
                if (!ex.getMessage().contains("SQLITE_ERROR")) {
                    System.out.println("Schema fix warning (supplier_payments method): " + ex.getMessage());
                }
            }

            try {
                jdbcTemplate.execute(
                        "ALTER TABLE customer_deferred_payments MODIFY COLUMN method VARCHAR(20) NOT NULL DEFAULT 'CASH'");
                System.out.println(
                        "Schema fix executed: ALTER TABLE customer_deferred_payments MODIFY COLUMN method VARCHAR(20)");
            } catch (Exception ex) {
                if (!ex.getMessage().contains("SQLITE_ERROR")) {
                    System.out.println("Schema fix warning (customer_deferred_payments method): " + ex.getMessage());
                }
            }

            try {
                jdbcTemplate.execute(
                        "UPDATE supplier_payments SET method = 'CASH' WHERE method NOT IN ('CASH','INSTAPAY','VISA','VODAFONE_CASH') OR method IS NULL");
                System.out.println("Schema fix executed: normalized supplier_payments.method values");
            } catch (Exception ex) {
                if (!ex.getMessage().contains("no such table")) {
                    System.out.println("Schema fix warning (normalize supplier_payments method): " + ex.getMessage());
                }
            }

            try {
                jdbcTemplate.execute(
                        "UPDATE customer_deferred_payments SET method = 'CASH' WHERE method NOT IN ('CASH','INSTAPAY','VISA','VODAFONE_CASH') OR method IS NULL");
                System.out.println("Schema fix executed: normalized customer_deferred_payments.method values");
            } catch (Exception ex) {
                if (!ex.getMessage().contains("no such table")) {
                    System.out.println(
                            "Schema fix warning (normalize customer_deferred_payments method): " + ex.getMessage());
                }
            }

            migratePaymentMethodConstraintsForSqlite();
        } catch (Exception e) {
            System.out.println("Schema fix warning (might be already fixed or table missing): " + e.getMessage());
        }
    }

    private void migratePaymentMethodConstraintsForSqlite() {
        try {
            String dbProduct;
            try (java.sql.Connection connection = jdbcTemplate.getDataSource().getConnection()) {
                dbProduct = connection.getMetaData().getDatabaseProductName();
            }
            if (dbProduct == null || !dbProduct.toLowerCase().contains("sqlite")) {
                return;
            }
        } catch (Exception e) {
            System.out.println("Schema fix warning (db product detection): " + e.getMessage());
            return;
        }

        try {
            String supplierSchema = jdbcTemplate.queryForObject(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='supplier_payments'",
                    String.class);
            if (supplierSchema != null && (supplierSchema.contains("'BANK'") || supplierSchema.contains("'OTHER'"))) {
                jdbcTemplate.execute("PRAGMA foreign_keys = OFF");
                jdbcTemplate.execute("ALTER TABLE supplier_payments RENAME TO supplier_payments_old");
                jdbcTemplate.execute("""
                        CREATE TABLE supplier_payments (
                            payment_id integer PRIMARY KEY,
                            amount numeric(38,2) not null,
                            method varchar(20) not null check (method in ('CASH','INSTAPAY','VISA','VODAFONE_CASH')),
                            notes TEXT,
                            payment_date timestamp not null,
                            supplier_id bigint not null
                        )
                        """);
                jdbcTemplate.execute("""
                        INSERT INTO supplier_payments (payment_id, amount, method, notes, payment_date, supplier_id)
                        SELECT payment_id,
                               amount,
                               CASE
                                   WHEN method IN ('CASH','INSTAPAY','VISA','VODAFONE_CASH') THEN method
                                   ELSE 'CASH'
                               END,
                               notes,
                               payment_date,
                               supplier_id
                        FROM supplier_payments_old
                        """);
                jdbcTemplate.execute("DROP TABLE supplier_payments_old");
                jdbcTemplate.execute("PRAGMA foreign_keys = ON");
                System.out.println("Schema fix executed: migrated supplier_payments method constraint for SQLite");
            }
        } catch (DataAccessException ex) {
            if (!ex.getMessage().contains("no such table")) {
                System.out.println("Schema fix warning (sqlite supplier_payments migration): " + ex.getMessage());
            }
            jdbcTemplate.execute("PRAGMA foreign_keys = ON");
        }

        try {
            String deferredSchema = jdbcTemplate.queryForObject(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='customer_deferred_payments'",
                    String.class);
            if (deferredSchema != null && (deferredSchema.contains("'BANK'") || deferredSchema.contains("'OTHER'"))) {
                jdbcTemplate.execute("PRAGMA foreign_keys = OFF");
                jdbcTemplate.execute("ALTER TABLE customer_deferred_payments RENAME TO customer_deferred_payments_old");
                jdbcTemplate.execute("""
                        CREATE TABLE customer_deferred_payments (
                            payment_id integer PRIMARY KEY,
                            amount numeric(38,2) not null,
                            method varchar(20) not null check (method in ('CASH','INSTAPAY','VISA','VODAFONE_CASH')),
                            notes TEXT,
                            payment_date timestamp not null,
                            customer_id bigint not null,
                            worker_id bigint
                        )
                        """);
                jdbcTemplate.execute("""
                        INSERT INTO customer_deferred_payments (payment_id, amount, method, notes, payment_date, customer_id, worker_id)
                        SELECT payment_id,
                               amount,
                               CASE
                                   WHEN method IN ('CASH','INSTAPAY','VISA','VODAFONE_CASH') THEN method
                                   ELSE 'CASH'
                               END,
                               notes,
                               payment_date,
                               customer_id,
                               worker_id
                        FROM customer_deferred_payments_old
                        """);
                jdbcTemplate.execute("DROP TABLE customer_deferred_payments_old");
                jdbcTemplate.execute("PRAGMA foreign_keys = ON");
                System.out.println("Schema fix executed: migrated customer_deferred_payments method constraint for SQLite");
            }
        } catch (DataAccessException ex) {
            if (!ex.getMessage().contains("no such table")) {
                System.out.println(
                        "Schema fix warning (sqlite customer_deferred_payments migration): " + ex.getMessage());
            }
            jdbcTemplate.execute("PRAGMA foreign_keys = ON");
        }
    }
}
