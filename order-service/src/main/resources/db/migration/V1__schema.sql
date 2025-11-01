CREATE TABLE IF NOT EXISTS orders (
  id BIGSERIAL PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'NEW'
);

CREATE TABLE IF NOT EXISTS order_items (
  order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id BIGINT NOT NULL,
  quantity INTEGER NOT NULL,
  price DOUBLE PRECISION NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox_event (
  id BIGSERIAL PRIMARY KEY,
  aggregate_type VARCHAR(64) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  type VARCHAR(64) NOT NULL,
  payload JSONB NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  published_at TIMESTAMP NULL
);
CREATE INDEX IF NOT EXISTS idx_outbox_unpublished ON outbox_event (published_at) WHERE published_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
