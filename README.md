## Bank System
A microservices-based banking system built with Spring Boot and modern cloud-native technologies.
The project simulates core banking operations such as account management, transactions, and user onboarding.

It integrates authentication, API gateway, service discovery, and resilience patterns.

![Architecture](img/architecture.png)

## Tech Stack
- **Language and build**: Java 17, Maven.
- **Framework**: Spring Boot 3.5.8, Spring Web, Spring Validation, Spring Boot Actuator, Spring DevTools, Lombok, ModelMapper.
- **Microservices**: Spring Cloud, Eureka Server, Spring Cloud Config Server, OpenFeign, Resilience4j.
- **Persistence**: PostgreSQL 15, Spring Data JPA, Flyway.
- **Messaging**: Apache Kafka 4.2, Spring Kafka, Kafka UI.
- **Security**: Spring Security, OAuth2, JWT, Keycloak 26, Keycloak Admin Client.
- **API Gateway**: Kong 3.6, decK.
- **Email**: Mailpit.
- **Testing**: JUnit 5, Spring Boot Test, Spring Security Test, Spring Kafka Test, Testcontainers.
- **Infrastructure and deployment**: Docker, Docker Compose, Kubernetes, Kustomize, kind.

## Features
### Registration Architecture
Registration starts in `user-service`, which keeps its own Postgres database and Keycloak in sync
and, once the KYC step succeeds, asks `account-service` (through Kafka) to open the user's account.

1. `POST /user/register` (public): creates the user in Keycloak and Postgres
   with status `REGISTER` and sends the verification email.
2. `PUT /user/register/me/profile`: requires status `REGISTER`; updates the
   user's names in Keycloak and moves to `PROFILE`.
3. `PUT /user/register/me/kyc`: requires status `PROFILE` and a verified email;
   assigns the `CUSTOMER_BASIC` role in Keycloak, moves to `KYC` and enqueues the
   `user-service.account-requested` Kafka event.
4. `PUT /user/register/me/financial`: requires status `KYC`; moves to `COMPLETE`.

#### Consistency between Postgres and Keycloak
Each step runs in a single database transaction. The DB changes are flushed **before** calling
Keycloak, so constraint violations are detected first; if a Keycloak call then fails, the
transaction rolls back and the step can be retried.

#### Transactional Outbox (KYC → account creation)
The account request is **not** sent to Kafka directly from the business logic. It uses the
*transactional outbox pattern*:

```
PUT /me/kyc ──► [ DB transaction ]
                  ├─ save user_kyc, role = CUSTOMER_BASIC, status = KYC
                  ├─ insert outbox_event (PENDING)        ◄── same transaction
                  └─ Keycloak: assign role + status
                       │ commit (or rollback of everything)
                       ▼
OutboxScheduler (every 2s) ──► EventOutbox.publishPending()
                  ├─ SELECT ... PENDING FOR UPDATE SKIP LOCKED
                  ├─ send to Kafka and wait for the broker ack
                  └─ mark SENT  (on error: attempts++, FAILED after 10)
                       ▼
account-service UserConsumer ──► createAccount (ignored if the user already has one)
```
