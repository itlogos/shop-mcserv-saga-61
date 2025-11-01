-- Create a sample NEW order with two items and publish an outbox event that will be picked up and sent to Kafka
INSERT INTO orders(id, customer_id, status) VALUES
  (1, 1, 'NEW')
ON CONFLICT DO NOTHING;

INSERT INTO order_items(order_id, product_id, quantity, price) VALUES
  (1, 1, 1, 699.0),
  (1, 3, 1, 349.0)
ON CONFLICT DO NOTHING;

INSERT INTO outbox_event(aggregate_type, aggregate_id, type, payload, created_at)
SELECT 'Order', '1', 'OrderCreated',
       '{"orderId":1,"customerId":1,"items":[{"productId":1,"quantity":1,"price":699.0},{"productId":3,"quantity":1,"price":349.0}]}',
       now()
WHERE NOT EXISTS (SELECT 1 FROM outbox_event WHERE aggregate_id='1' AND type='OrderCreated');
