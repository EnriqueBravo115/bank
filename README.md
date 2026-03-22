## Bank System
A microservices-based banking system built with Spring Boot and modern cloud-native technologies.
The project simulates core banking operations such as account management, transactions, and user onboarding.

It integrates authentication, API gateway, service discovery, and resilience patterns.

![Architecture](img/architecture.png)

## Tech Stack
- Java
- Spring Boot
- Spring Cloud
- Postgres
- Flyway 
- Resilience4j
- Docker
- Keycloak
- Kong
- Kafka
- JUnit
- Testcontainers

## Running the Project
Create the bank-maven-cache image, which contains the dependencies of all services:
```bash
docker build -f Dockerfile.maven-cache -t bank-maven-cache .
```
Finally, run the docker-compose:
```bash
docker compose up --build
```

## Features
### User Registration Flow
User registration is implemented using a custom Keycloak authentication flow composed of four steps:
1. Basic Info
2. Profile Info
3. KYC Info
4. Financial Info

### Transaction Types
The platform currently supports four types of financial transactions:
1. Transfer
2. Purchase
3. Service Payment
4. Withdrawal
