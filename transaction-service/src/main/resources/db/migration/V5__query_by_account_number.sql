-- Queries now filter by account_number (the source account resolved by account-service) instead of
-- source_identifier, so a transfer made with the CLABE shows up when querying by account number.

-- Rows from before V4 have no account_number. Those created with the account number can be filled
-- in here; those created with a CLABE need account-service and keep NULL (not returned by queries).
UPDATE transactions SET account_number = source_identifier
WHERE account_number IS NULL AND identifier_type = 'ACCOUNT';

DROP INDEX idx_transactions_source_status_date;
DROP INDEX idx_transactions_owner;

CREATE INDEX idx_transactions_account_status_date
    ON transactions (account_number, transaction_status, transaction_date DESC);
CREATE INDEX idx_transactions_owner
    ON transactions (owner_id, account_number, transaction_status);
