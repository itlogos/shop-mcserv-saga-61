# store-service
**Endpoints**
- `GET /api/products`
- `POST /admin/products` (ROLE_ADMIN)
- `DELETE /admin/products/{id}` (ROLE_ADMIN)
- `POST /api/products/{id}/reserve?qty=Q` (ROLE_CUSTOMER/ADMIN)
