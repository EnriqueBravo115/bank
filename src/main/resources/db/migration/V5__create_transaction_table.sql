CREATE TABLE transaction (
    id                      SERIAL PRIMARY KEY,
    amount                  DECIMAL,
    description             VARCHAR(255), 
    transaction_date        TIMESTAMP,
    transaction_type        VARCHAR(255),
    transaction_status      VARCHAR(255),
    source_account_id       INT REFERENCES account(id) ON DELETE CASCADE,
    target_account_id       INT REFERENCES account(id) ON DELETE CASCADE,
    original_transaction_id INT REFERENCES transaction(id) ON DELETE CASCADE
)
