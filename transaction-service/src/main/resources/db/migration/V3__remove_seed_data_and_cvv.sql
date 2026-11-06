-- V2 seeded example rows in every environment. They used explicit ids without moving the
-- sequence, so the first real inserts collided with them; tests now create their own data.
DELETE FROM transfer_transaction WHERE id IN (SELECT id FROM transactions WHERE transaction_code LIKE 'TXN-2024-%');
DELETE FROM purchase_transaction WHERE id IN (SELECT id FROM transactions WHERE transaction_code LIKE 'TXN-2024-%');
DELETE FROM service_transaction WHERE id IN (SELECT id FROM transactions WHERE transaction_code LIKE 'TXN-2024-%');
DELETE FROM withdrawal_transaction WHERE id IN (SELECT id FROM transactions WHERE transaction_code LIKE 'TXN-2024-%');
DELETE FROM transactions WHERE transaction_code LIKE 'TXN-2024-%';

SELECT setval(pg_get_serial_sequence('transactions', 'id'), COALESCE(MAX(id), 0) + 1, false) FROM transactions;

-- The card security code must never be stored (PCI-DSS); it is only forwarded to account-service.
ALTER TABLE purchase_transaction DROP COLUMN cvv;

ALTER TABLE transactions ADD CONSTRAINT uk_transactions_transaction_code UNIQUE (transaction_code);

-- Every query filters by source identifier and status, and most order by date.
CREATE INDEX idx_transactions_source_status_date
    ON transactions (source_identifier, transaction_status, transaction_date DESC);
