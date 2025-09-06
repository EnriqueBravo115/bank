CREATE TABLE account
(
    id              BIGINT PRIMARY KEY,
    balance         DECIMAL,
    account_number  VARCHAR(20),
    currency        VARCHAR(2),
    user_id         BIGINT REFERENCES users(id) ON DELETE SET NULL
);
