CREATE TABLE transaction (
    id                      BIGINT PRIMARY KEY,
    amount                  DECIMAL,
    description             VARCHAR(255), 
    transaction_date        TIMESTAMP,
    transaction_type        VARCHAR(255),
    transaction_status      VARCHAR(255),
    source_account_id       BIGINT REFERENCES account(id) ON DELETE CASCADE,
    target_account_id       BIGINT REFERENCES account(id) ON DELETE CASCADE,
    original_transaction_id BIGINT REFERENCES transaction(id) ON DELETE SET NULL
);
