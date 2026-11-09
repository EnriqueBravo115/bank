-- The account that receives a transfer (and its owner), so the receiver sees the money it got.
-- Older transfers only know the target account number: their owner is unknown (admins still see them).
ALTER TABLE transactions ADD COLUMN counterparty_account_number VARCHAR(255);
ALTER TABLE transactions ADD COLUMN counterparty_owner_id VARCHAR(255);

UPDATE transactions t
SET counterparty_account_number = tt.target_account_number
FROM transfer_transaction tt
WHERE tt.id = t.id;

CREATE INDEX idx_transactions_counterparty ON transactions (counterparty_account_number, transaction_status);
CREATE INDEX idx_transactions_account ON transactions (account_number, transaction_status);

-- PENDING transactions that ran out of attempts wait for an admin (next_attempt_at is NULL).
CREATE INDEX idx_transactions_stuck ON transactions (transaction_date)
    WHERE transaction_status = 'PENDING' AND next_attempt_at IS NULL;
