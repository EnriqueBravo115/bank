-- Withdrawal fee (debited along with the amount) and how the customer was verified.
ALTER TABLE movement ADD COLUMN fee NUMERIC(19, 4);
ALTER TABLE movement ADD COLUMN verification_method VARCHAR(50);

-- Limits add up the COMPLETED movements of an account by type since the start of the period.
CREATE INDEX idx_movement_limit_usage
    ON movement (source_account_number, movement_type, created_at)
    WHERE status = 'COMPLETED';

-- One limit per account, type and period: keep the latest one of any duplicates.
DELETE FROM transaction_limit older
USING transaction_limit newer
WHERE newer.account_id = older.account_id
  AND newer.limit_type = older.limit_type
  AND newer.time_period = older.time_period
  AND newer.id > older.id;

ALTER TABLE transaction_limit
    ADD CONSTRAINT uk_transaction_limit_account_type_period UNIQUE (account_id, limit_type, time_period);
