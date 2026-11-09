-- account_number was "ACC-" + millis: two accounts opened in the same millisecond got the same
-- number. Any repeated number is replaced (all but the oldest account) before making it UNIQUE.
UPDATE account a
SET account_number = 'ACC-' || LPAD(a.id::TEXT, 16, '0')
WHERE EXISTS (SELECT 1 FROM account b WHERE b.account_number = a.account_number AND b.id < a.id);

ALTER TABLE account ADD CONSTRAINT uk_account_account_number UNIQUE (account_number);

-- Only two balance types are written: OPENING and MOVEMENT (movements were stored as INTRADAY).
UPDATE account_balance SET balance_type = 'MOVEMENT' WHERE balance_type <> 'OPENING';

ALTER TABLE account_balance
    ADD CONSTRAINT ck_account_balance_type CHECK (balance_type IN ('OPENING', 'MOVEMENT'));
