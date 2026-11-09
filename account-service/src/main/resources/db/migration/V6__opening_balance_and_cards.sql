-- Accounts created before the opening balance existed had no account_balance row, so every
-- movement failed with "balance not found". They start at 0, like new accounts.
INSERT INTO account_balance (account_id, balance, balance_type, creation_date)
SELECT a.id, 0, 'OPENING', a.creation_date
FROM account a
WHERE NOT EXISTS (SELECT 1 FROM account_balance b WHERE b.account_id = a.id);

-- The current balance is the latest row (by id) of the account.
CREATE INDEX idx_account_balance_account_latest ON account_balance (account_id, id DESC);

-- The card security code is never stored (PCI-DSS): it is derived from the card number and the
-- expiration date with a secret key, and checked by deriving it again (CardSecurityCode).
ALTER TABLE card DROP COLUMN cvv;

CREATE INDEX idx_card_account ON card (account_id);
