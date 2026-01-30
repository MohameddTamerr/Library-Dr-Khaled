-- Suppliers module schema + sample data

CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) NULL,
    email VARCHAR(120) NULL,
    address TEXT NULL,
    status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    total_purchases DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_paid DECIMAL(12,2) NOT NULL DEFAULT 0,
    balance_due DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS purchases (
    purchase_id INT NOT NULL AUTO_INCREMENT,
    supplier_id INT NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    payment_status ENUM('PAID','PARTIAL','UNPAID') NOT NULL,
    purchase_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT NULL,
    PRIMARY KEY (purchase_id),
    INDEX idx_purchases_supplier_id (supplier_id),
    CONSTRAINT fk_purchases_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS supplier_payments (
    payment_id INT NOT NULL AUTO_INCREMENT,
    supplier_id INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    method ENUM('CASH','BANK','OTHER') NOT NULL DEFAULT 'CASH',
    notes TEXT NULL,
    PRIMARY KEY (payment_id),
    INDEX idx_supplier_payments_supplier_id (supplier_id),
    CONSTRAINT fk_supplier_payments_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Sample suppliers
INSERT INTO suppliers (name, phone, email, address, status, total_purchases, total_paid, balance_due)
VALUES
('Alpha Books', '01001112233', 'alpha@books.com', 'Cairo - Downtown', 'ACTIVE', 2000.00, 1500.00, 500.00),
('PaperLine', '01002223344', 'sales@paperline.com', 'Giza - Dokki', 'ACTIVE', 1200.00, 1200.00, 0.00),
('Stationery Hub', '01003334455', 'contact@stationeryhub.com', 'Alexandria - Miami', 'ACTIVE', 750.00, 400.00, 350.00),
('Bright Office', '01004445566', 'info@brightoffice.com', 'Tanta - City Center', 'ACTIVE', 3000.00, 1600.00, 1400.00),
('Novelty Supplies', '01005556677', 'hello@noveltysupplies.com', 'Mansoura - East', 'INACTIVE', 400.00, 50.00, 350.00);

-- Sample purchases
INSERT INTO purchases (supplier_id, total_amount, paid_amount, payment_status, purchase_date, notes)
VALUES
(1, 1000.00, 700.00, 'PARTIAL', NOW(), 'Initial stock order'),
(1, 1000.00, 800.00, 'PARTIAL', NOW(), 'Second order'),
(2, 1200.00, 1200.00, 'PAID', NOW(), 'Full payment at purchase'),
(3, 500.00, 100.00, 'PARTIAL', NOW(), 'Notebooks shipment'),
(3, 250.00, 200.00, 'PARTIAL', NOW(), 'Pens batch'),
(4, 2000.00, 500.00, 'PARTIAL', NOW(), 'Seasonal supplies'),
(4, 1000.00, 500.00, 'PARTIAL', NOW(), 'Restock'),
(5, 400.00, 0.00, 'UNPAID', NOW(), 'Trial order');

-- Sample supplier payments
INSERT INTO supplier_payments (supplier_id, amount, payment_date, method, notes)
VALUES
(3, 100.00, NOW(), 'CASH', 'Partial payment'),
(4, 600.00, NOW(), 'BANK', 'Bank transfer'),
(5, 50.00, NOW(), 'OTHER', 'Settlement');
