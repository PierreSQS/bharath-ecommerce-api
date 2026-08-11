-- ---------------------------------------------------------------------------
-- V2: populate the schema with reference data (categories, products) and
-- sample customers. Orders, order_items and payments are intentionally left
-- empty — create those through the API so order-number generation, stock
-- reduction and the cascade save all run as designed.
-- ---------------------------------------------------------------------------

-- categories
INSERT INTO categories (name, slug, description, created_at, updated_at) VALUES
('Electronics',    'electronics',    'Gadgets, accessories and devices',   NOW(), NOW()),
('Books',          'books',          'Printed and reference books',        NOW(), NOW()),
('Clothing',       'clothing',       'Apparel and everyday wear',          NOW(), NOW()),
('Home & Kitchen', 'home-kitchen',   'Homeware and kitchen essentials',    NOW(), NOW());

-- products
INSERT INTO products (name, description, price, sku, stock_quantity, active, category_id, created_at, updated_at) VALUES
('Wireless Mouse',              'Ergonomic 2.4GHz wireless mouse',            24.99, 'ELEC-MOU-001', 150, TRUE, 1, NOW(), NOW()),
('Mechanical Keyboard',         'Tactile mechanical keyboard, RGB backlit',   79.99, 'ELEC-KEY-002',  80, TRUE, 1, NOW(), NOW()),
('USB-C Hub',                   '7-in-1 USB-C multiport adapter',             39.99, 'ELEC-HUB-003',  60, TRUE, 1, NOW(), NOW()),
('Noise-Cancelling Headphones', 'Over-ear ANC wireless headphones',          199.99, 'ELEC-HDP-004',  40, TRUE, 1, NOW(), NOW()),
('Clean Code',                  'A Handbook of Agile Software Craftsmanship', 32.50, 'BOOK-CLN-001', 200, TRUE, 2, NOW(), NOW()),
('The Pragmatic Programmer',    'Your Journey to Mastery',                    41.00, 'BOOK-PRG-002', 120, TRUE, 2, NOW(), NOW()),
('Cotton T-Shirt',              'Soft 100% cotton crew-neck tee',             15.00, 'CLTH-TSH-001', 300, TRUE, 3, NOW(), NOW()),
('Hooded Sweatshirt',           'Fleece-lined pullover hoodie',               45.00, 'CLTH-HOD-002',  90, TRUE, 3, NOW(), NOW()),
('Stainless Steel Bottle',      'Insulated 750ml water bottle',               18.99, 'HOME-BTL-001', 250, TRUE, 4, NOW(), NOW()),
('Ceramic Coffee Mug',          '350ml glazed ceramic mug',                   12.50, 'HOME-MUG-002', 180, TRUE, 4, NOW(), NOW());

-- customers
INSERT INTO customers (first_name, last_name, email, phone, address, created_at) VALUES
('John',  'Doe',   'john.doe@example.com',   '+1-202-555-0101', '123 Maple Street, Springfield', NOW()),
('Jane',  'Smith', 'jane.smith@example.com', '+1-202-555-0142', '88 Oak Avenue, Riverdale',       NOW()),
('Ravi',  'Kumar', 'ravi.kumar@example.com', '+91-90000-12345', '12 MG Road, Bengaluru',          NOW());
