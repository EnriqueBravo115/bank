-- owner_id: Keycloak id of the source account's owner, so customers only see their own transactions.
-- account_number: the source account resolved by account-service (the request may use a CLABE).
ALTER TABLE transactions ADD COLUMN owner_id VARCHAR(255);
ALTER TABLE transactions ADD COLUMN account_number VARCHAR(255);

-- A transaction is stored as PENDING before calling account-service; while the answer is unknown
-- (timeout, account-service down) it is retried with backoff using the same idempotency key.
ALTER TABLE transactions ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE transactions ADD COLUMN next_attempt_at TIMESTAMP;

ALTER TABLE transfer_transaction ADD COLUMN target_account_number VARCHAR(255);

CREATE INDEX idx_transactions_pending ON transactions (next_attempt_at) WHERE transaction_status = 'PENDING';
CREATE INDEX idx_transactions_owner ON transactions (owner_id, source_identifier, transaction_status);
