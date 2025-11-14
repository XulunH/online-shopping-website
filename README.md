# Online Shopping Backend (Spring Boot Microservices)

This repository contains four backend services for an online shopping system:
- account-service (MySQL)
- item-service (MongoDB)
- order-service (Cassandra)
- payment-service (MySQL)

Services communicate synchronously via RestTemplate and asynchronously via Kafka events. Authentication is handled via JWT issued by account-service.

## Tech Stack
- Spring Boot, Spring Web, Spring Security (JWT), Validation
- Spring Data JPA (MySQL), Spring Data MongoDB, Spring Data Cassandra
- Kafka (spring-kafka)
- OpenAPI/Swagger (springdoc-openapi)
- JUnit 5, Mockito
- JaCoCo for coverage (≥ 30% enforced per service)
- Docker Compose for one-click infra + services

## One-click Run (Docker)
Requirements: Docker + Docker Compose

```bash
docker-compose up --build -d
```

URLs (after startup):
- Account Swagger: http://localhost:8081/swagger-ui/index.html
- Item Swagger:    http://localhost:8082/swagger-ui/index.html
- Order Swagger:   http://localhost:8083/swagger-ui/index.html
- Payment Swagger: http://localhost:8084/swagger-ui/index.html

Infra started by compose: MySQL, MongoDB, Cassandra, Zookeeper, Kafka.

## Authentication
1) Register an account
```bash
curl -X POST http://localhost:8081/api/v1/accounts/register \
  -H "Content-Type: application/json" \
  -d '{
        "email": "user@example.com",
        "username": "user1",
        "password": "Passw0rd!",
        "shippingAddress": { "line1":"123 Main", "city":"NYC","state":"NY","zip":"10001","country":"US" },
        "billingAddress":  { "line1":"123 Main", "city":"NYC","state":"NY","zip":"10001","country":"US" }
      }'
```
2) Login to get JWT
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Passw0rd!"}' | jq -r .token)
echo "$TOKEN"
```

Use the token in other requests with:
```bash
-H "Authorization: Bearer $TOKEN"
```

## Quick Demo Flow
1) Create an item in item-service
```bash
curl -X POST http://localhost:8082/api/v1/items \
  -H "Content-Type: application/json" \
  -d '{
        "upc":"UPC-001","name":"Sample Item","unitPrice": 12.50,
        "pictureUrls": [], "availableUnits": 20
      }'
```
2) Create an order (order-service)
```bash
curl -X POST http://localhost:8083/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"upc":"UPC-001","quantity":2}]}'
```
Note the returned `id` as ORDER_ID.

3) Submit a payment (payment-service)
```bash
curl -X POST http://localhost:8084/api/v1/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
        \"orderId\":\"ORDER_ID\",
        \"amount\":12.50*2,
        \"idempotencyKey\":\"key-001\"
      }"
```
4) Order auto-completes via Kafka
```bash
curl http://localhost:8083/api/v1/orders/ORDER_ID
# status should become COMPLETED
```
5) Cancel the order (triggers refund event to payment-service)
```bash
curl -X POST http://localhost:8083/api/v1/orders/ORDER_ID/cancel \
  -H "Authorization: Bearer $TOKEN"
```

## Testing & Coverage
Run all tests (from repo root):
```bash
chmod +x run-tests.sh
./run-tests.sh
```

Run a single module:
```bash
cd payment-service && ./mvnw clean verify
```

Coverage reports (HTML) are generated and saved at:
- `account-service/target/site/jacoco/index.html`
- `item-service/target/site/jacoco/index.html`
- `order-service/target/site/jacoco/index.html`
- `payment-service/target/site/jacoco/index.html`

Builds fail if any module coverage < 30%.

## Kafka Topics
- `payment.events`: produced by payment-service on payment success; consumed by order-service (completes order).
- `order.events`: produced by order-service on cancel; consumed by payment-service (sets payment refunded).

## Environment Notes
- JWT secret is provided via properties/environment; see `docker-compose.yml`.
- Service-to-service base URLs are configured via properties (and overridden in docker-compose).


