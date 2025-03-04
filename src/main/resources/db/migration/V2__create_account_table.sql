CREATE TABLE account
(
    id              SERIAL PRIMARY KEY,
    account_number  VARCHAR(255),
    account_type    VARCHAR(255),
    currency        VARCHAR(3),
    status          VARCHAR(255),
    balance         DOUBLE PRECISION,
    creation_date   TIMESTAMP DEFAULT current_timestamp,
    user_id         INT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
