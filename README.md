# Wallet Withdrawals Service

A Java project that simulates wallet withdrawal operations using a hexagonal architecture.  
It is split into three layers: **domain**, **application**, and **infrastructure**.

---

## Project setup

**Technologies used**
- Java 21  
- Spring Boot 3.5.5  
- PostgreSQL with jOOQ  
- Flyway for database migrations  
- JUnit 5, Mockito, Testcontainers, WireMock, RestAssured  
- Docker Compose for local setup

**Structure**
```

domain/          → business logic and entities
application/     → use cases and input ports
infrastructure/  → REST API, database, Spring configuration

```

**Design choices**
- Hexagonal architecture to keep business logic independent from frameworks.
- jOOQ for type-safe SQL generation based on migration scripts.
- Domain events for internal communication.
- Transactional event publishing to keep database and event states consistent.

---

## How to run locally

1. **Start dependencies**
   ~~~bash
   docker compose up -d
```

2. **Build and generate code**

   ```bash
   ./gradlew clean build
   ```

3. **Run the application**

   ```bash
   ./gradlew :infrastructure:bootRun
   ```

   The API will be available at:

   ```
   http://localhost:8080
   ```

---

## How to test the endpoint

Create a withdrawal:

```bash
curl -X POST http://localhost:8080/api/v1/wallet_withdraw \
  -H "Content-Type: application/json" \
  -d '{
        "userId": 1,
        "amount": 100.00,
        "recipientFirstName": "John",
        "recipientLastName": "Doe",
        "recipientRoutingNumber": "123456789",
        "recipientNationalId": "12345678901",
        "recipientAccountNumber": "987654321"
      }'
```

Example response:

```json
{
  "transactionId": "uuid",
  "status": "CREATED",
  "createdAt": "2025-10-25T10:00:00Z"
}
```

Check the withdrawal:

```bash
curl http://localhost:8080/api/v1/wallet_withdraw/{id}
```

---

## Design decisions and trade-offs

| Decision                       | Reason                                           | Trade-off                         |
| ------------------------------ | ------------------------------------------------ | --------------------------------- |
| Hexagonal architecture         | Separation between business logic and frameworks | Adds boilerplate (ports/adapters) |
| Domain events                  | Clean decoupling between modules                 | Harder to trace execution         |
| Transactional event publishing | Ensures atomic persistence and events            | Increases transaction complexity  |
| jOOQ instead of JPA            | Full SQL control and schema safety               | Requires manual query handling    |
| Testcontainers                 | Realistic integration tests                      | Slower startup time               |

---

## Run tests

```bash
./gradlew test
```

Integration tests use PostgreSQL and WireMock through Testcontainers.

```
```
