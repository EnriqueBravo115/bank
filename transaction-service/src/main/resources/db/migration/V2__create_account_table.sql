CREATE TABLE account
(
    id              SERIAL PRIMARY KEY,
    balance         DECIMAL,
    account_number  VARCHAR(255),
    account_type    VARCHAR(255),
    currency        VARCHAR(3),
    status          VARCHAR(255),
    creation_date   TIMESTAMP DEFAULT current_timestamp,
    user_id         INT REFERENCES users(id) ON DELETE SET NULL
);
