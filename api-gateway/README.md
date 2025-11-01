# api-gateway
Routes:
- `/store/**` -> store-service
- `/customer/**` -> customer-service
- `/order/**` -> order-service
Secured by JWT (Keycloak). Redis rate limiter example on store route.
