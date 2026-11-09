# Pending tasks

## account-service

### Security (critical)

- [x] **Any customer could move money out of any account**
  - `/api/v1/account/movement/*` was reachable through Kong by any `CUSTOMER_BASIC`.
  - It now requires the `SERVICE` role, which only the `transaction-service` Keycloak client has (client credentials). transaction-service checks account ownership before moving the money.

- [x] **Movement amounts were not validated**
  - The `Client*Request` records have `@NotBlank`/`@NotNull @Positive` (`@PositiveOrZero` for the fee) and the controller uses `@Valid`: a negative or zero amount is `400`.

- [x] **IDOR in account lookups**
  - `GET /me` returns the account of the JWT `sub`; `get-by-account-number` and `get-by-clabe` are for `ADMIN` and `SERVICE` (transaction-service resolves accounts and owners with them), `get-by-email` is `ADMIN` only.

### Features

- [x] **No balance was created when opening an account**
  - `createAccount` stores a 0 `account_balance` (`OPENING`) in the same transaction as the account.
  - `V6` creates that opening balance for the accounts that had none.

- [ ] **Deposits / credits**
  - `AccountBalanceController` is commented out and `AccountBalanceServiceImpl.createAccountBalance` throws `UnsupportedOperationException`.

- [x] **Cards**
  - `/api/v1/account/card`: `POST` issues a debit card for the account of the `sub` (in the name of the `name` claim; `409` if it already has an `ACTIVE`/`BLOCKED` one, `422` if the account is not `OPEN`). It is the only response with the full number and the cvv. `GET /me` lists them masked; `POST /{id}/block|unblock|cancel` for the owner or `ADMIN` (another user's card is `404`, an invalid transition `409`, `CANCELLED` is final).
  - 16-digit number with BIN `412345` and a Luhn check digit (`CardNumberGenerator`); it expires at the end of the month, 5 years ahead.
  - The cvv is not stored (`V6` drops the column): `CardSecurityCode` derives it with HMAC-SHA256 from the number and the expiration date using `card.cvv-secret` (`CARD_CVV_SECRET`), like an issuer's CVK, and compares it in constant time. Changing the secret invalidates the cvv of every card.
  - Purchases require `sourceCardNumber` (`400`) and are declined (`422`, `Card declined: …`) if the card is not linked to the account, is not `ACTIVE`, is expired or the cvv does not match. transaction-service only sends the cvv on its first attempt: a retry that never reached account-service before is declined.
  - `GET /get-by-card-number/{cardNumber}` (`ADMIN`, `SERVICE`) returns the card's account.

- [ ] **Cards: pending**
  - The full card number (PAN) is stored in plain text; PCI-DSS requires encrypting or tokenizing it.
  - `card.cvv-secret` has a development-only default in config-server: outside local development `CARD_CVV_SECRET` must be set (in k8s, in the `secretGenerator`).

- [ ] **Incomplete transaction limits**
  - There is no way to create them.
  - `isWithinDailyLimit` compares the amount of a single operation, not the sum of the day.
  - `max_transactions` is not used.

- [ ] **Ignored movement fields**
  - `transactionFee` in withdrawals, `SecurityVerificationMethod` and the currency (money can be transferred between accounts with different currencies).

### Consistency

- [x] **Race condition on the balance**
  - Every movement (and its reversal) locks its accounts with `SELECT ... FOR UPDATE` (`AccountRepository.lockByAccountNumber`) before reading the balance, in account number order so two opposite transfers cannot deadlock.
  - The current balance is the latest row by `id`, not by `creation_date` (two rows could share a date); index `(account_id, id DESC)`.
  - `MovementControllerTest`: 10 concurrent debits of 100 over 500 → exactly 5 are applied (without the lock all 10 were), and concurrent opposite transfers do not deadlock.

- [ ] **Poorly modeled balance types**
  - Every movement is stored as `INTRADAY`, but `hasSufficientFundsIncludingHolds` looks for `AVAILABLE`/`ON_HOLD`; `checkHolds` is always `false`; "the latest by date" mixes types.

- [x] **Movements without idempotency**
  - Every movement requires the `Idempotency-Key` header and is recorded in the `movement` table (`V5`, `UNIQUE` key): repeating the key returns the stored result without moving the money again; two concurrent requests with the same key → `409`. A rejection (`422`) is not stored, so a retry is evaluated again.
  - `POST /movement/{key}/reverse`: returns the money of a `COMPLETED` movement (`REVERSED`) or, if the key never arrived, blocks it (`CANCELLED`) so a late request is not applied.

- [ ] **Account identifiers**
  - `account_number = "ACC-" + millis` is not `UNIQUE` and can repeat.
  - `ClabeGenerator` should compute the real check digit.

### Quality

- [ ] **Dead code**
  - `AccountServiceImpl.updateEmail(accountNumber, …)` (`// TODO`, unused; `updateEmailByUserId` already covers it).
  - `FeignConfiguration` without any Feign client.

- [x] **Wrong error message in `get-by-email`**
  - Now `"Account not found with email: <email>"` (and `clabe`/`userId` in their lookups).

- [ ] **Tests**
  - Added `MovementControllerTest` and `AccountControllerTest` (real PostgreSQL with Testcontainers: security, validation, idempotency, reversal). The Kafka consumers are still missing.

## transaction-service

### Security and consistency (critical)

- [x] **Distributed inconsistency when creating transactions**
  - `TransactionSettlement` first stores the transaction as `PENDING` (commit) and then calls account-service with the `transactionCode` as `Idempotency-Key`.
  - Only answers that prove the money did not move are final (`422` → `DECLINED`; `404`/`400`/`401`/`403` → `FAILED`). A timeout, 5xx, `409` or an open circuit leave it `PENDING`.
  - `PendingTransactionScheduler` (every `transaction.reconciliation.interval`, 5 s) sends the due `PENDING` transactions again with the same key and backoff (5 s, 10 s… up to 15 min), with `FOR UPDATE SKIP LOCKED` for several replicas. Migration `V4` (`attempts`, `next_attempt_at`, partial index).
  - Feign with `connectTimeout` 2 s and `readTimeout` 5 s (previously the 60 s default): past that time the transaction stays `PENDING` and the backoff starts counting when the attempt ends.

- [x] **IDOR in queries and creation**
  - Every transaction stores `owner_id` (the `userId` of the source account, according to account-service). All queries, analytics and support filter by the JWT `sub` except for `ADMIN`; another user's `/{transactionCode}` is `404`.
  - When creating, the source account must belong to the user (`403`); the target must exist (`404`). If account-service does not answer the lookup, `503` and nothing is recorded.

- [x] **`IdentifierType` was not respected**
  - `AccountResolver` looks up by account number, CLABE or card depending on the type and always sends the `accountNumber` to account-service.
  - `CARD` is resolved with `GET /api/v1/account/get-by-card-number/{cardNumber}` (previously `400`): it works as the source of transfers and in queries and analytics. Error messages only show the last 4 digits (`**** 1111`).

### Features

- [x] **Queries filtered by the identifier the transaction was created with**
  - Queries, analytics and support filter by `account_number`, not by `sourceIdentifier`: an account sees its transactions made with the account number, the CLABE or a card.
  - Optional `sourceIdentifierType` parameter (`ACCOUNT` by default, without calling account-service). With `CLABE` or `CARD`, `AccountResolver.accountNumberOf` resolves it to its account (`404` if it does not exist, `503` if account-service does not answer).
  - `/query/by-sources?sourceIdentifiers=` becomes `/query/by-accounts?accountNumbers=`.
  - `V5` fills `account_number` for the rows from before `V4` created with the account number (those created with a CLABE stay `NULL`) and moves the indexes to `account_number`.

- [x] **Withdrawal endpoint**
  - `POST /api/v1/transaction/create/withdrawal`, with validation (`@Positive`, `@NotBlank`, `@NotNull`) like purchases and service payments.

- [x] **Cancellation and reversal**
  - `POST /api/v1/transaction/{code}/cancel` (owner or `ADMIN`): `PENDING` only; account-service undoes it if it was applied or blocks the key → `CANCELLED` / `REVERSED`.
  - `POST /api/v1/transaction/{code}/reverse` (`ADMIN`): `COMPLETED` only → `REVERSED`. If the target account no longer has the money, `409`; if account-service does not answer, `503` and nothing changes.

### Bugs

- [x] **Queries returned `transactionCode` and `description` as `null`**
  - `TransactionCommonProjection` did not have those getters; ModelMapper had nothing to copy them from.

- [x] **`/analytics/max-by-type` answered 500**
  - `BasicMapper.convertToResponseList` called `lists.contains(null)`, which throws `NullPointerException` on `List.of(...)` lists.

- [x] **Purchases, service payments and withdrawals did not store `sourceIdentifier`**
  - They did not show up in any query. The source account is now stored with `identifierType = ACCOUNT`.

- [x] **Purchases and withdrawals did not send the account to account-service**
  - `PurchaseRequest` and `WithdrawalRequest` require `sourceAccountNumber`, and `ClientPurchaseRequest`/`ClientWithdrawalRequest` now match account-service's. The card is still only stored in `purchase_transaction`.

- [x] **Ignored filters**
  - `/date-range` did not filter by `status`; `/max` always used `COMPLETED`; `/status-year` filtered in memory (it now uses `findAllBySourceIdentifierAndYear`).

- [x] **`/{transactionCode}` and `/max` answered `200` with `null`**
  - `GlobalExceptionHandler` with `TransactionNotFoundException` → `404` and `{status, message, timestamp}`, like user-service.

- [x] **`process` wrapped everything in `RuntimeException`**
  - Not anymore: account-service rejections arrive as DECLINED/FAILED through the fallback and any other error propagates and rolls back (the transaction is not recorded).

- [x] **The cvv was stored** in `purchase_transaction.cvv` (PCI-DSS)
  - `V3` drops the column; the cvv is only forwarded to account-service. Feign goes from `FULL` logging (which wrote the bodies, card and cvv included) to `BASIC`.

- [x] **Resilience4j configuration**
  - `AccountServiceIntegrationTest` starts a fake account-service (real HTTP, no new dependencies) and checks that the `@CircuitBreaker` of `AccountClient` does run the fallback: 422 → DECLINED with the message, 404 and 5xx → FAILED, and the JSON that is sent.
  - Removed `feign.circuitbreaker.enabled` (old name and not needed: the fallback comes from the Resilience4j aspect) and the `resilience4j.retry` block, which nothing used.

### Quality

- [x] **Dead code**
  - Removed: `TransactionHelper`, `KafkaTopicConstants`, `ErrorMessage`, `AccountServiceException`, `ApiRequestException`, `AccountStatus`, `TransferResponse`, the 8 `FilterStatus*Request`, `TransferTransactionRepository`, the empty `cancelTransaction` and `@Service` on the interfaces.
  - Removed dependencies: `spring-kafka`, `spring-kafka-test` and `spring-boot-starter-oauth2-client`.
  - `TestConstants` moved to `src/test`.

- [x] **Test data in production migrations**
  - `V3__remove_seed_data_and_cvv.sql` deletes the `V2` rows and resets the `transactions.id` sequence: `V2` inserted explicit ids without moving it, so the first real inserts collided with them.
  - It also adds `UNIQUE (transaction_code)` and the `(source_identifier, transaction_status, transaction_date)` index used by every query.

- [x] **Responses in the user-service style**
  - Records as direct JPQL projections (`TransactionResponse`, `TransactionSummaryResponse`, `TransactionResultResponse.from`); removed the 3 projections, their 3 DTOs, `BasicMapper`, ModelMapper and `HeaderResponse`.
  - `/query/all` returns a `PagedModel` with validated `page`/`size` (`Paging`, at most 100) like user-service. `/analytics/max-by-type` returns one transaction per type, not a list.
  - `/query/by-sources` went from `GET` with a body to `?sourceIdentifiers=a,b`.
  - `PurchaseRequest` and `ServiceRequest` no longer carry `transactionType` (it was ignored).

- [x] **Tests following the user-service pattern**
  - Integration tests per controller with real PostgreSQL; each class creates its own data (`TransactionFixtures`) instead of using the `V2` seed.
  - Removed the Mockito unit tests of the services (`TransactionQueryServiceTest` started a container it did not use) and `TransactionServiceTestHelper`.
  - New: `TransactionCreationControllerTest` (`@MockitoBean AccountClient`: what is sent to account-service and what is stored with COMPLETED/DECLINED, 400 validation, 401/403) and `AccountClientFallbackTest` (422 → DECLINED with the message, 404/401/5xx → FAILED); analytics `type-summary`, `average-days` and `partition-by-amount`.

## user-service

- [x] **Partial failure in Keycloak**
  - In the KYC step, if `assignRole` succeeds and `updateRegistrationStatus` fails, the database rolls back but the role stays assigned in Keycloak. Same risk in `updateEmail` and in the role change.
  - Fixed with `RollbackCompensation`: before calling Keycloak, how to undo it is registered, and it runs if the transaction rolls back (because of a Keycloak or commit failure). It covers KYC (removes `CUSTOMER_BASIC` and sets `registration_status` back to `PROFILE`), role change (restores the previous tier), email change (restores the email, `username` and its verification) and block/unblock (restores `enabled`; closed sessions do not come back).
  - If the compensation also fails, only a `log.error` is left: there is no automatic retry.

- [x] **Outbox maintenance**
  - Review/retry `FAILED` events (admin endpoint or job) and purge old `SENT` ones.
  - `ADMIN` endpoints: `GET /api/v1/user/outbox/failed` (paginated) and `POST /api/v1/user/outbox/{id}/retry` (back to `PENDING` with 0 attempts; `409` if it is not `FAILED`, `404` if it does not exist).
  - Daily cleanup (`outbox.cleanup.cron`, 03:00 by default) of the `SENT` events older than `outbox.cleanup.sent-retention` (`P7D`).
  - Exponential backoff between attempts (5 s, 10 s, 20 s… up to 15 min): an event becomes `FAILED` after about 40 min of failures, not after ~20 s of Kafka being down.
  - Per-user order: while an event is `FAILED` or waiting for its backoff, the following events of the same user wait. Previously they were published, and retrying the old one applied it out of order (e.g. a block after the unblock).

- [ ] **Outbox event order with several replicas**
  - `FOR UPDATE SKIP LOCKED` avoids sending the same row twice, but if two replicas publish at the same time they can send two events of the same user in a different order. Options: an advisory lock so a single replica publishes, or a lock per `aggregate_id`.

- [x] **CURP/RFC were not cross-checked**
  - Nothing checked that they matched each other or `birthday`.
  - `KycConsistency` in the KYC step (`400` with the reason): the `YYMMDD` date of the CURP and RFC must be the `birthday`, character 17 of the CURP must match the century (digit up to 1999, letter from 2000) and the 4 letters must match (accepting Ñ→X in the CURP and the inconvenient-word rule, which the CURP applies to the 2nd letter and the RFC to the 4th).
  - Pending, if more is wanted: the CURP check digit, the CURP sex against `gender` and the 4 letters against the names and surnames (rules for compound names such as MARÍA/JOSÉ and particles such as DE/LA).

- [ ] **(Optional) User closure / deregistration**
  - With its event to close the account in account-service.

## Build / Docker

- [ ] **(Optional) Change each service's `context` in `docker-compose.yml` to its own folder**
  - Now that each service is independent, it can be `context: ./account-service`.
  - In that case, the Dockerfile would have to copy `pom.xml` and `src` without the service prefix.

- [ ] **Kong caches the IP of the services**
  - If a single service is recreated or restarted (`docker compose up -d <service>`, `docker stop/start`), its IP changes and Kong answers `502` until it is restarted (`docker compose restart kong`). Option: a low `KONG_DNS_VALID_TTL` on the `kong` service.

- [ ] **CI/CD pipeline (GitHub Actions)**
  - A workflow was built and removed for now. Proposed design:
    - `test`: `mvn verify` per service with JDK 17 and `actions/setup-java` (`cache: maven`).
    - `image`: build with BuildKit layer cache (`type=gha`, `mode=max`), Trivy scan and push to GHCR on `main`.
    - `e2e`: kind cluster + `kubectl apply -k k8s` + `k8s/smoke-test.sh`.
  - The `/root/.m2` cache mount in the Dockerfiles does not persist on ephemeral runners: if a service's `src` changes, its image downloads the dependencies again.
  - When building it: Trivy first in report-only mode and then blocking; build only the services that changed (`dorny/paths-filter`).

## Maven

- [ ] **Local build with JDK 25 fails because of Lombok**
  - Since JDK 23, `javac` does not run annotation processors unless they are explicitly enabled.
  - It affects `account-service`, `user-service` and `transaction-service`: `cannot find symbol` errors on getters, setters and `builder()`.
  - It does not happen in Docker because the image uses JDK 17.
  - Options:
    - Build with `mvn package -Dmaven.compiler.proc=full`.
    - Add `<maven.compiler.proc>full</maven.compiler.proc>` to the `<properties>` of each pom.
    - Use JDK 17/21 locally.

- [ ] **Versions duplicated across services**
  - Spring Boot `3.5.8`, Spring Cloud `2025.0.0`, `modelmapper 3.2.0` and `testcontainers 1.21.4` are repeated in every pom.
  - Upgrading means changing them in all of them, or moving testcontainers to its BOM (`testcontainers-bom`).

## Kubernetes (`k8s/`)

- [ ] **Plain-text secrets in `k8s/kustomization.yaml`**
  - The PostgreSQL and Keycloak admin credentials are `literals` in the `secretGenerator`.
  - For anything beyond local development: Sealed Secrets, External Secrets or SOPS.

- [ ] **Ephemeral Kafka data**
  - The broker has no volume (same as in docker-compose); topics are lost if the pod restarts.

- [ ] **Test cross-service flows in Kubernetes**
  - transaction-service needs to reach Keycloak (`KEYCLOAK_HOST`) to request its service token.
  - Verified: health, endpoints of the 3 services through Kong with a Keycloak token, Eureka registration and kafka-ui.
  - Pending: the registration flow (user-service → Kafka → account-service) and Feign (transaction-service → account-service).

## Security

- [ ] **Gmail app password in the git history**
  - It is no longer in the code, but it is still in the history: **it must be revoked** in the Google account.
