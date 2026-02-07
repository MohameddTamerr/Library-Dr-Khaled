-- Add return_condition column to orders table
ALTER TABLE orders ADD COLUMN return_condition VARCHAR(10) NULL;
