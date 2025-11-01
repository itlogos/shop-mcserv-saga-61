INSERT INTO customer(name, phone) VALUES
 ('Alice Brown', '+353800000001'),
 ('Bob Smith', '+353800000002'),
 ('Charlie Johnson', '+353800000003'),
 ('Diana Prince', '+353800000004'),
 ('Evan Davis', '+353800000005')
ON CONFLICT DO NOTHING;
