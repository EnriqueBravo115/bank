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
### User Service
`user-service` owns the customer: onboarding, identity data (profile, KYC, financial info) and the
user's lifecycle. It keeps its own Postgres database in sync with Keycloak, which handles login,
roles and email verification, and tells `account-service` about changes through Kafka.

#### Registration Architecture
Registration starts in `user-service`, which keeps its own Postgres database and Keycloak in sync
and, once the KYC step succeeds, asks `account-service` (through Kafka) to open the user's account.

1. `POST /user/register` (public): creates the user in Keycloak and Postgres
   with status `REGISTER` and sends the verification email. Duplicate email or phone → `409`.
2. `PUT /user/register/me/profile`: requires status `REGISTER`; saves names, gender and
   birthday (adults only), updates the user's names in Keycloak and moves to `PROFILE`.
3. `PUT /user/register/me/kyc`: requires status `PROFILE` and a verified email; validates
   CURP and RFC (format, match with each other and with the birthday, not already registered),
   assigns the `CUSTOMER_BASIC` role in Keycloak, moves to `KYC` and enqueues the
   `user-service.account-requested` Kafka event.
4. `PUT /user/register/me/financial`: requires status `KYC`; moves to `COMPLETE`.

After step 1 the user logs in, and the remaining steps act on the JWT owner (`/me`).
During registration the user can also resend the verification email
(`POST /user/register/me/verify-email`) and fix a mistyped email (`PUT /user/register/me/email`).

#### Account Management
- **Contact changes** (customer): `PUT /user/me/email` and `PUT /user/me/phone`. A new email is
  also the Keycloak username, has to be verified again and is propagated to `account-service`.
- **Customer tier** (admin): `PUT /user/{userId}/role` switches between `CUSTOMER_BASIC`,
  `CUSTOMER_PREMIUM` and `CUSTOMER_BUSINESS`. `CUSTOMER_BASIC` is always kept in Keycloak and the
  tier is added on top; `ADMIN` cannot be granted through the API.
- **Block / unblock** (admin): `PUT /user/{userId}/active` disables the Keycloak user, ends its
  sessions and freezes the account in `account-service` (unblocking reopens it).

#### Queries
- **Customer**: `GET .../me` for the user, profile, KYC and financial info, resolved from the JWT,
  so a customer can only read their own data.
- **Admin**: lookups by id, email, RFC or CURP, profile search by name or registration status,
  and the list of active users. Lists are paginated (`page`, `size` up to 100).

#### Access Control
Endpoints are protected with Keycloak JWTs and realm roles (`CUSTOMER_*`, `ADMIN`). On each
request a filter also rejects blocked users (`403`), even if their token has not expired yet,
and, outside the registration steps, users whose registration has not reached `KYC`.

#### Consistency between Postgres and Keycloak
Each operation runs in a single database transaction. The DB changes are flushed **before** calling
Keycloak, so constraint violations are detected first. If a Keycloak call (or the commit) then
fails, the transaction rolls back and the changes already made in Keycloak are undone
(`RollbackCompensation`), so the step can be retried.

#### Transactional Outbox (user-service → account-service)
Events (`account-requested`, `user-status-changed`, `user-email-changed`) are **not** sent to
Kafka directly from the business logic. They use the *transactional outbox pattern*:

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
                  └─ mark SENT  (on error: retry with backoff, FAILED after 10 attempts)
                       ▼
account-service UserConsumer ──► createAccount (ignored if the user already has one)
```

- A user's events are delivered in order: while one is `FAILED` or waiting to be retried,
  the user's later events wait.
- Admins list `FAILED` events (`GET /user/outbox/failed`) and put them back in the queue
  (`POST /user/outbox/{id}/retry`).
- A daily job deletes `SENT` events older than 7 days.
