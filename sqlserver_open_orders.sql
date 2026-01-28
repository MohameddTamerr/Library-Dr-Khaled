-- SQL Server schema for cashier open orders

IF OBJECT_ID('dbo.open_orders', 'U') IS NULL
BEGIN

    CREATE TABLE dbo.open_orders (
        open_order_id INT IDENTITY(1,1) PRIMARY KEY,
        customer_id INT NULL,
        delivery_man_id INT NULL,
        status NVARCHAR(20) NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_open_orders_created DEFAULT (SYSUTCDATETIME()),
        updated_at DATETIME2 NULL
    );
    ALTER TABLE dbo.open_orders
        ADD CONSTRAINT FK_open_orders_customer
            FOREIGN KEY (customer_id) REFERENCES dbo.customers(customer_id);
    ALTER TABLE dbo.open_orders
        ADD CONSTRAINT FK_open_orders_delivery_man
            FOREIGN KEY (delivery_man_id) REFERENCES dbo.users(user_id);
END

IF OBJECT_ID('dbo.open_order_items', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.open_order_items (
        open_order_item_id INT IDENTITY(1,1) PRIMARY KEY,
        open_order_id INT NOT NULL,
        product_id INT NOT NULL,
        quantity INT NOT NULL,
        price DECIMAL(10,2) NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_open_order_items_created DEFAULT (SYSUTCDATETIME())
    );
    ALTER TABLE dbo.open_order_items
        ADD CONSTRAINT FK_open_order_items_order
            FOREIGN KEY (open_order_id) REFERENCES dbo.open_orders(open_order_id) ON DELETE CASCADE;
    ALTER TABLE dbo.open_order_items
        ADD CONSTRAINT FK_open_order_items_product
            FOREIGN KEY (product_id) REFERENCES dbo.products(product_id);
END

-- Optional: add address column for customers if missing
IF COL_LENGTH('dbo.customers', 'address') IS NULL
BEGIN
    ALTER TABLE dbo.customers ADD address NVARCHAR(255) NULL;
END

-- Ensure delivery men are stored in users with role = 'delivery_men'
-- Example:
-- INSERT INTO dbo.users (role, full_name, phone, username, password) VALUES ('delivery_men', N'مندوب 1', N'01000000000', 'del1', 'pass');
