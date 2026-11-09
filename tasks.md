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

- [x] **Deposits / credits**
  - `POST /movement/deposit` (`SERVICE`, idempotent like every movement): credits an `OPEN` account; the movement has no source account. Reversing it debits the account, `422` if it no longer has the money.
  - transaction-service: `POST /api/v1/transaction/create/deposit` (`ADMIN` only), target by account number, CLABE or card; the `DEPOSIT` transaction belongs to the account's owner.
  - The stub `AccountBalanceController`/`AccountBalanceService` (`UnsupportedOperationException`) was removed.

- [x] **Cards**
  - `/api/v1/account/card`: `POST` issues a debit card for the account of the `sub` (in the name of the `name` claim; `409` if it already has an `ACTIVE`/`BLOCKED` one, `422` if the account is not `OPEN`). It is the only response with the full number and the cvv. `GET /me` lists them masked; `POST /{id}/block|unblock|cancel` for the owner or `ADMIN` (another user's card is `404`, an invalid transition `409`, `CANCELLED` is final).
  - 16-digit number with BIN `412345` and a Luhn check digit (`CardNumberGenerator`); it expires at the end of the month, 5 years ahead.
  - The number and a random 3-digit cvv are stored with the card in plain text (practice project, no PCI-DSS): `V8` adds back the `cvv` column that `V6` had dropped.
  - Purchases require `sourceCardNumber` (`400`) and are declined (`422`, `Card declined: …`) if the card is not linked to the account, is not `ACTIVE`, is expired or the cvv does not match. transaction-service stores the cvv with the purchase (`V6`) and sends it on every attempt.
  - `GET /get-by-card-number/{cardNumber}` (`ADMIN`, `SERVICE`) returns the card's account.

- [x] **Transaction limits**
  - `/api/v1/account/limit`: `PUT` creates or replaces the limit of an account for a type and period (`ADMIN`; one per account, type and period, `V7` constraint), `GET /me` (`CUSTOMER_BASIC`), `GET /{accountNumber}` and `DELETE /{id}` (`ADMIN`). The currency is the account's.
  - `TransactionLimitService.check` adds up the `COMPLETED` movements of the type since the start of the calendar period (`DAILY` midnight, `WEEKLY` Monday, `MONTHLY` the 1st) and checks both `maxAmount` and `maxTransactions` (`422`). It runs with the account locked, so concurrent movements cannot both fit. Reversed movements and withdrawal fees do not count.
  - The old `isWithinDailyLimit`/`isWithinTransactionLimit` were removed.

- [x] **Ignored movement fields**
  - Withdrawals debit amount + `transactionFee` (funds are checked for both; a reversal refunds both) and store the fee and the `SecurityVerificationMethod` (required) in `movement` (`V7`).
  - Every movement carries its `currency` (required; transaction-service sends the transaction's): it must be the currency of every account it touches, both of a transfer, `422` otherwise. There is no currency conversion.

### Consistency

- [x] **Race condition on the balance**
  - Every movement (and its reversal) locks its accounts with `SELECT ... FOR UPDATE` (`AccountRepository.lockByAccountNumber`) before reading the balance, in account number order so two opposite transfers cannot deadlock.
  - The current balance is the latest row by `id`, not by `creation_date` (two rows could share a date); index `(account_id, id DESC)`.
  - `MovementControllerTest`: 10 concurrent debits of 100 over 500 → exactly 5 are applied (without the lock all 10 were), and concurrent opposite transfers do not deadlock.

- [x] **Poorly modeled balance types**
  - `BalanceType` only has what is written: `OPENING` (0 when the account is opened) and `MOVEMENT` (after a movement or its reversal; it was `INTRADAY`). The current balance is the latest row of any type. There are no holds: `hasSufficientFundsIncludingHolds`, `getAvailableBalance` and the query by type were removed.
  - `V9` renames the old rows and adds a `CHECK` on `balance_type`.

- [x] **Movements without idempotency**
  - Every movement requires the `Idempotency-Key` header and is recorded in the `movement` table (`V5`, `UNIQUE` key): repeating the key returns the stored result without moving the money again; two concurrent requests with the same key → `409`. A rejection (`422`) is not stored, so a retry is evaluated again.
  - `POST /movement/{key}/reverse`: returns the money of a `COMPLETED` movement (`REVERSED`) or, if the key never arrived, blocks it (`CANCELLED`) so a late request is not applied.

- [x] **Account identifiers**
  - `AccountNumberGenerator`: `"ACC-"` + 16 random digits (instead of `"ACC-" + millis`), checked to be free before saving; `V9` renames any repeated number and makes `account_number` `UNIQUE`.
  - `ClabeGenerator` already computed the Banxico check digit (weights 3, 7, 1); `GeneratorsTest` checks it against real CLABEs. The 11 account digits are now random (`SecureRandom`) instead of the clock.

### Quality

- [x] **Dead code**
  - Removed `AccountServiceImpl.updateEmail(accountNumber, …)` (with `AccountAlreadyExistsException` and `existsByEmail`, only used there), `FeignConfiguration` and the `spring-cloud-starter-openfeign` dependency (account-service has no Feign client).

- [x] **Wrong error message in `get-by-email`**
  - Now `"Account not found with email: <email>"` (and `clabe`/`userId` in their lookups).

- [x] **Tests**
  - `MovementControllerTest`, `AccountControllerTest`, `CardControllerTest`, `LimitControllerTest` (real PostgreSQL with Testcontainers) and unit tests of the services and generators.
  - `UserConsumerTest`: the user-service events through an embedded broker (KRaft; ZooKeeper took 2 min resolving the host name) with the production JSON serialization and `auth-user-id` header: account opening (once, with its opening balance), freeze/reopen and email change.

### Review findings

- [x] **Customers cannot see their balance**
  - No endpoint returns the balance: `GET /api/v1/account/me` (`AccountDetailedResponse`) has no balance field, and `account_balance` is only read internally. A customer can move money but never knows how much they have.
  - Add the current balance (latest `account_balance` row) to `/me`, or a `GET /me/balance`.
  - Done: `GET /api/v1/account/me/balance` (CUSTOMER_BASIC) and `GET /api/v1/account/balance/{accountNumber}` (ADMIN) return `BalanceResponse` (account number, latest balance, currency, date).

- [x] **A malformed Kafka message blocks the consumer forever**
  - `value-deserializer` is `JsonDeserializer` directly (config-server `account-service.yml`), not wrapped in `ErrorHandlingDeserializer`. A message that cannot be deserialized fails in `poll()`, before any listener or error handler, so the container retries the same offset in a loop and every later event of that partition (account openings, freezes) is stuck.
  - Wrap it in `ErrorHandlingDeserializer` (`spring.deserializer.value.delegate.class`).
  - Done: `ErrorHandlingDeserializer` wraps `JsonDeserializer` (config-server and test `application.yml`); a record that cannot be read goes straight to `<topic>.DLT` without retries (`KafkaErrorHandlingConfig`).

- [x] **A failed account opening is dropped silently**
  - There is no error handler configured: Spring Kafka's default `DefaultErrorHandler` retries 10 times with no delay and then only logs and skips the record. If the database is down for a few seconds when `user-service.account-requested` arrives, the account is never created and the user stays at KYC without an account.
  - Configure a `DefaultErrorHandler` with backoff and a dead-letter topic (`DeadLetterPublishingRecoverer`).
  - Done: `KafkaErrorHandlingConfig`: `DefaultErrorHandler` with exponential backoff (1s up to 30s, for 5 minutes by default, `account.kafka.retry.*`) and then `DeadLetterPublishingRecoverer` to `<topic>.DLT`.

- [x] **Events of different topics can be applied out of order**
  - `UserConsumer` has one listener container per topic, so `user-status-changed` or `user-email-changed` can be consumed before the `account-requested` event of the same user. A user blocked right after KYC gets an `OPEN` account (the freeze finds no account and is ignored, then the account is created), and an email change made right after KYC is lost (the account is created with the old email from the event).
  - Options: one topic per user aggregate (ordered by key), or have the account opening re-check the current state (e.g. user-service sends `active` and the email with each event, or account-service asks user-service).
  - Done: A status or email change for a user without an account is kept in `pending_user_update` (`V10`) and applied when the account is opened (inactive → `FROZEN`, latest email), then deleted.

- [x] **Email lookups are case-sensitive**
  - `GET /get-by-email/{email}` matches the email exactly, while user-service stores it as typed: `Ana@Mail.com` is not found as `ana@mail.com`.
  - Done: `findByEmail` compares with `LOWER()` on both sides.

- [x] **The same Idempotency-Key with a different payload returns the old result**
  - `once()` only looks at the key: a second request with the same key but another amount or account gets the first movement's result without any error. transaction-service always sends a new UUID, so it does not happen today, but a caller bug would be silent. Store a hash of the request and answer `409`/`422` when it differs.
  - Done: `once()` compares the stored movement (type, accounts, amount, total debit) with the new one and answers `422` (`IdempotencyKeyReusedException`) when they differ.

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

- [x] **Feign logged the request bodies**
  - Feign goes from `FULL` logging (which wrote the bodies, card and cvv included) to `BASIC`. `V3` had dropped `purchase_transaction.cvv`; `V6` adds it back so a `PENDING` purchase is retried with its cvv.

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

### Review findings

- [x] **Incoming transfers are not visible to the receiving account**
  - Every query, analytics and support endpoint filters by `account_number` (the source account) and `owner_id` (the owner of the source account). The target account of a transfer never sees the money it received: its owner cannot find the transaction in history, keyword search, totals or by its code. Only deposits (stored with the deposited account) show up as credits.
  - Include transfers whose `transfer_transaction.target_account_number` is the account (and let its owner read them), with a direction (`DEBIT`/`CREDIT`) in `TransactionResponse` so totals do not add incoming and outgoing money together.
  - Done: `V7`: `counterparty_account_number` and `counterparty_owner_id` (backfilled from `transfer_transaction`). Queries return what the account sent plus the COMPLETED transfers it received; `TransactionResponse.direction` is `DEBIT` or `CREDIT`. Analytics and support only use the account's own operations, so totals never mix money in and out.

- [x] **`count-by-month` merges different years**
  - `countTransactionsByMonth` groups by `Month` only: January 2025 and January 2026 are added together. Group by `YearMonth`.
  - Done: Grouped by `YearMonth` (keys such as `2024-01`), sorted.

- [x] **PENDING transactions are retried forever**
  - `TransactionSettlement` caps the delay at 15 minutes but not the number of attempts: if account-service keeps failing (5xx), a transaction stays `PENDING` indefinitely and nobody is told. Add a maximum (or a deadline) after which it is flagged for an admin, like user-service's outbox `FAILED` state.
  - Done: After `TransactionSettlement.MAX_ATTEMPTS` (20, about 3 hours) the transaction stays `PENDING` with no next attempt and a reason for the admin. `GET /api/v1/transaction/stuck` lists them and `POST /api/v1/transaction/{code}/retry` (ADMIN) tries again with a new round of attempts; it can still be cancelled.

- [x] **Range queries accept inverted ranges and LIKE wildcards**
  - `date-range` and `amount-range` do not check `start <= end` / `min <= max` (they just return nothing). `keyword` does not escape `%` and `_`, so `%` matches every transaction.
  - Done: `400` when `startDate > endDate` or `minAmount > maxAmount`; `%`, `_` and `\` are escaped in `keyword` (`ESCAPE '\\'`).

### Tests (review)

- [x] **Query and analytics fixtures only have outgoing transactions of one year**
  - `TransactionAnalyticsControllerTest` uses dates from 2024 only, so `count-by-month` merging years is not caught, and no test has a transfer seen from the receiving account. Add those cases when fixing the bugs above.
  - Done: `TransactionFixtures.incomingTransfer`; tests for received transfers (history, keyword, by code from both sides, not readable by a third user, left out of analytics), `count-by-month` across 2024 and 2025, inverted ranges, literal wildcards and stuck PENDING transactions.

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

### Review findings

- [x] **Hard-coded account type and currency**
  - The KYC step always requests a `SAVING` account in `MX` (`RegisterServiceImpl.updateKycData`): the customer cannot choose, and `CHECKING`, `US` and `EU` are never used although account-service supports them.
  - Done: `UserKycDataRequest.accountType` and `currency` are optional; `SAVING` in `MX` by default.

- [x] **Email lookups are case-sensitive**
  - Registration and email changes check duplicates with `IgnoreCase`, but `GET /user/get-by-email/{email}` (`UserRepository.getUserByEmail`) compares exactly, and the `UNIQUE` constraint on `users.email` is case-sensitive. Normalize emails to lowercase when they are stored.
  - Done: Emails are stored lowercase (the requests normalize them, `V9` lowercases existing rows) and `getUserByEmail` compares with `LOWER(:email)`.

- [x] **The phone number is unique without its country code**
  - `existsByPhoneNumber` and the `UNIQUE` constraint ignore `phone_code`: `+1 5512345678` and `+52 5512345678` are different phones but the second one is rejected.
  - Done: `V9`: `UNIQUE (phone_code, phone_number)` instead of `users_phone_number_key`; `existsByPhoneCodeAndPhoneNumber` on registration and phone changes.

- [x] **Second surname is mandatory**
  - `UserProfileRequest.secondSurname` is `@NotBlank` and `second_surname` is `NOT NULL`: people with a single surname (common among foreigners) cannot register. `searchByName` concatenates it too, so it would also need to handle `NULL`.
  - Done: Optional in the request (blank is stored as null), nullable column (`V9`), `COALESCE` in `searchByName` and the Keycloak last name is only the first surname when there is no second one.

- [x] **The KYC document type is optional**
  - `UserKycDataRequest.documentType` has no `@NotNull` and the column is nullable: a KYC can be completed without saying which document was checked.
  - Done: `@NotNull documentType` (`400`). The column stays nullable for older rows.

- [x] **Keycloak is updated without compensation in the profile and financial steps**
  - `updateProfile` and `updateFinancialInfo` update Keycloak (name, `registration_status`) before the commit, with no `RollbackCompensation`, unlike the KYC step and the role/active/email changes. If the commit fails, the token says `PROFILE`/`COMPLETE` while the database still has the previous step. A retry fixes it, but the two systems disagree meanwhile.
  - `register` has the same gap at commit time: the Keycloak user is only deleted if the failure happens inside the `try`, and the verification email is sent before the commit.
  - Done: `RollbackCompensation` in `updateProfile` (name back to `PENDING`, `registration_status` `REGISTER`) and `updateFinancialInfo` (`KYC`). `register` deletes the Keycloak user on any rollback, the commit included, and sends the verification email after the commit.

- [x] **Two endpoints change the email**
  - `PUT /api/v1/register/me/email` and `PUT /api/v1/user/me/email` call the same service method; the first one has no role check (any authenticated user, for registrations in progress). Keep one, or document why both exist.
  - Done: Only `PUT /api/v1/user/me/email` remains, for any authenticated user; `RegisterStatusFilter` lets it through for registrations in progress.

- [x] **Blocked users keep reading other services until their token expires**
  - Only user-service checks `active` on each request (`RegisterStatusFilter`). account-service and transaction-service accept the blocked user's access token for up to its lifespan (5 minutes): the account is `FROZEN` so no money moves, but queries still work. Acceptable with short tokens; document it or add token introspection.
  - Done: Accepted risk, documented in the README and `RegisterStatusFilter`: the account is `FROZEN`, so only queries work until the 5-minute token expires.

### Tests (review)

- [x] **The Keycloak tests run against a different realm**
  - `user-service/src/test/resources/keycloak/realm-export.json` is a separate, older copy (no `SERVICE` role, no `transaction-service` client, no `admin-user`), while the README says `k8s/keycloak/realm-export.json` is the only copy. Tests can pass with a realm that is not the deployed one. Load `k8s/keycloak/realm-export.json` from the tests (e.g. `withCopyFileToContainer` from the repository path).
  - Done: The test copy was removed; `pom.xml` adds `k8s/keycloak/realm-export.json` as a test resource, so the Keycloak container imports the deployed realm.

- [x] **The production `SecurityConfig` is never tested**
  - `SecurityConfig` is disabled in the test profiles: `TestKeycloakConfig` is a copy of it (and already differs: no `/actuator/**` rule for `ADMIN`), and `TestPostgresConfig` permits everything and disables `RegisterStatusFilter`. A change to the real security rules is not caught by any test. Use the real `SecurityConfig` with `spring-security-test`'s `jwt()`, as account-service and transaction-service do.
  - Done: `@Profile` removed from `SecurityConfig`; `TestKeycloakConfig` and `TestPostgresConfig` deleted. Tests send `jwt()` with `registration_status`, and new tests check `401` without a token, `403` for registrations in progress and `/actuator/**` for admins only.

- [x] **The published event format is not checked against the consumer**
  - `EventOutboxTest` mocks `UserEventProducer`, and account-service's `UserConsumerTest` builds its own events. Nothing checks that what user-service sends (topic, `__TypeId__`, `auth-user-id`, JSON fields) is what account-service reads; renaming a field on one side would break account opening without failing a test. Add a test that sends through `UserEventProducer` to an embedded broker and deserializes with account-service's classes (or a shared contract).
  - Done: Shared contract in `contracts/user-events/*.json`. `UserEventContractTest` (user-service) builds each event from its file, sends it through `UserEventProducer` to an embedded KRaft broker and compares topic, `__TypeId__`, `auth-user-id` and the JSON; account-service's `UserConsumerTest` consumes the same files.

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
