CREATE TABLE account
(
    id              SERIAL PRIMARY KEY,
    balance         DECIMAL,
    account_type    VARCHAR(255),
    account_status  VARCHAR(255),
    currency        VARCHAR(3),
    user_id         INT REFERENCES users(id) ON DELETE SET NULL
);
