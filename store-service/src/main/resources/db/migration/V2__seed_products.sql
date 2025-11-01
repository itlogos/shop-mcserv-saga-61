INSERT INTO product(name, price, quantity) VALUES
 ('Apple iPhone 13', 699.0, 25),
 ('Samsung Galaxy S23', 749.0, 18),
 ('Sony WH-1000XM5', 349.0, 40),
 ('Apple MacBook Air M2', 1199.0, 12),
 ('Logitech MX Master 3S', 109.0, 60)
ON CONFLICT DO NOTHING;
