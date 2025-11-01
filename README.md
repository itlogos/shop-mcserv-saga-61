# Shop Microservices Starter

**Stack**
- Java 21 + Spring Boot 3.3
- Spring Cloud Gateway
- Keycloak (OIDC) with realm: `shop`
- Redis (cache + rate limiting)
- Kafka (Bitnami)
- Postgres (separate DB per service + Keycloak DB)
- React + Vite web app

**Services**
- `store-service` — products: id, name, price, quantity (with reservation endpoint)
- `customer-service` — customers: id, name, phone
- `order-service` — orders referencing customers and product items; reserves stock via `store-service`

**Users/Roles**
- Realm: `shop`
- Roles: `ROLE_CUSTOMER`, `ROLE_ADMIN`
- Example users will be created by Keycloak import:
  - `customer1 / customer1` (ROLE_CUSTOMER)
  - `admin1 / admin1` (ROLE_ADMIN)

**How to run (dev)**
1. `docker compose up -d --build`
2. Wait ~1–2 minutes until `keycloak`, `kafka`, `postgres` containers are healthy.
3. Access:
   - API Gateway: http://localhost:8080
   - Store API: http://localhost:8081
   - Customer API: http://localhost:8082
   - Order API: http://localhost:8083
   - Web App: http://localhost:5173
   - Keycloak: http://localhost:8085  (realm: `shop`, client: `frontend`)

**JWT**
- Gateway & services validate tokens issued by Keycloak realm `shop` using the issuer `http://keycloak:8080/realms/shop`.
- For local manual testing you may disable auth by setting `SECURITY_DISABLED=true` in service envs.

**Kafka topics**
- `order.created`

**Redis usage**
- `store-service`: cache product list
- `api-gateway`: RedisRateLimiter (per-route example)

**Build locally (optional)**
- Install JDK 21 & Maven
- `mvn -q -DskipTests -f ./pom.xml clean package`

---

See each service `README.md` for API examples.


## Seed data
В БД автоматически создаются записи при первом запуске (если таблицы пустые):

- **store-service / products (5 шт):**
  - Apple iPhone 13 — 699.00 — qty 25
  - Samsung Galaxy S23 — 749.00 — qty 18
  - Sony WH-1000XM5 — 349.00 — qty 40
  - Apple MacBook Air M2 — 1199.00 — qty 12
  - Logitech MX Master 3S — 109.00 — qty 60

- **customer-service / customers (5 шт):**
  - Alice Brown (+353800000001)
  - Bob Smith (+353800000002)
  - Charlie Johnson (+353800000003)
  - Diana Prince (+353800000004)
  - Evan Davis (+353800000005)

- **Keycloak users (realm `shop`):**
  - `customer1 / customer1` (ROLE_CUSTOMER)
  - `admin1 / admin1` (ROLE_ADMIN)



## Migrations (Flyway)
- `store-service`: `V1__schema.sql`, `V2__seed_products.sql`
- `customer-service`: `V1__schema.sql`, `V2__seed_customers.sql`
- JPA `ddl-auto` по умолчанию `validate` (схему создаёт Flyway).

## Seed Order
- В `order-service` включён опциональный сид-заказ (env `SEED_ORDER=true` в docker-compose). Создаётся один заказ после задержки ~20 секунд.

## Admin UI
- В `web-app` добавлена вкладка **Admin** (видна только роли `ROLE_ADMIN`), CRUD по товарам и клиентам.
- Вход под `admin1/admin1`.


## Outbox + Kafka
- `order-service` пишет события в таблицу `outbox_event` в одной транзакции с заказом.
- Планировщик `OutboxPublisher` периодически читает непубликованные записи и отправляет в Kafka `order.created`, после чего помечает `published_at`.
- `store-service` содержит пример Kafka consumer (`OrderCreatedConsumer`) и логирует события.

## Keycloak — группы и роли
- Добавлены группы realm: `admins` (ROLE_ADMIN), `customers` (ROLE_CUSTOMER).
- Пользователь `admin1` в группе `admins`, `customer1` в группе `customers`.


## Gateway authorization
Роуты в `api-gateway` защищены по ролям:  
- `/store/api/admin/**` — только `ROLE_ADMIN`  
- `/store/**`, `/customer/**`, `/order/**` — `ROLE_ADMIN` или `ROLE_CUSTOMER`

## Jwt role mapping
Во всех сервисах и в шлюзе добавлен конвертер, который маппит `realm_access.roles` → `ROLE_*` в `GrantedAuthority`.

## Mini-saga (демо)
Поток: `order-service` (Outbox → Kafka `order.created`) → `store-service` (consumer → публикует `order.confirmed`) → `order-service` (consumer → устанавливает статус `CONFIRMED`).  
Бизнес-резервирование пока остаётся синхронным при создании заказа — для демонстрации асинхронной цепочки подтверждение дублировано событиями.

## Debezium (опционально)
В `docker-compose.yml` добавлена закомментированная секция `debezium`. Можно включить для CDC из `order-db` (альтернатива встроенному OutboxPublisher). Требуется настроить коннектор через REST `POST /connectors`.


## Асинхронная сага (полная)
- `order-service` больше **не** вызывает `/store/.../reserve` синхронно.
- При создании заказа публикуется событие `order.created` (через Outbox).
- `store-service` проверяет наличие товара, уменьшает остаток и публикует `order.confirmed` или `order.failed`.
- `order-service` обновляет статус на `CONFIRMED`/`FAILED` при получении соответствующего события.

### UI
- Кнопка **Order 1** создаёт заказ через `/order/api/orders`. Подтверждение придёт асинхронно.

### Сидинг заказов
- `order-service` `V2__seed_order.sql` создаёт тестовый заказ и outbox-событие `OrderCreated` для запуска саги.
