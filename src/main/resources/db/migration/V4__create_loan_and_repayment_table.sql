CREATE TABLE loan
(
    id             SERIAL PRIMARY KEY,
    load_amount    INT,
    interest_rate  INT,
    start_date     TIMESTAMP DEFAULT current_timestamp,
    end_date       TIMESTAMP,
    status         VARCHAR(255),
    account_id     INT REFERENCES account (id) ON DELETE SET NULL
);

CREATE TABLE repayment
(
    id              SERIAL PRIMARY KEY,
    payment_date    TIMESTAMP,
    amount          DOUBLE PRECISION,
    loan_id         INT REFERENCES loan(id) ON DELETE SET NULL
);
