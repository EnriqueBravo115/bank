CREATE TABLE account
(
    id              INT NOT NULL,
    account_number  VARCHAR(255),
    account_type    VARCHAR(255),
    currency        VARCHAR(3),
    status          VARCHAR(255),
    balance         DOUBLE PRECISION,
    creation_date   TIMESTAMP DEFAULT current_timestamp
    PRIMARY KEY(id)
);


