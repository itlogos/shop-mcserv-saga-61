# order-service
**Endpoints**
- `GET /api/orders`
- `GET /api/orders/{id}`
- `POST /api/orders` — body example:
```json
{
  "customerId": 1,
  "items": [
    {"productId": 1, "quantity": 2, "price": 9.99}
  ]
}
```
Creates order and reserves stock in `store-service`.
Publishes Kafka event `order.created`.
