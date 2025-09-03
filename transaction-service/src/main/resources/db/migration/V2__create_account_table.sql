CREATE TABLE account
(
    id              BIGSERIAL PRIMARY KEY,
    balance         DECIMAL,
    account_number  VARCHAR(20),
    currency        VARCHAR(2),
    user_id         INT REFERENCES users(id) ON DELETE SET NULL
);
