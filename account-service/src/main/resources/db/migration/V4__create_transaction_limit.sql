CREATE TABLE transaction_limit (
    id               BIGSERIAL      NOT NULL,
    account_id       BIGINT         NOT NULL,
    max_amount       NUMERIC(19, 4) NOT NULL,
    max_transactions INTEGER        NOT NULL,
    currency         VARCHAR(10)    NOT NULL,
    limit_type       VARCHAR(50)    NOT NULL,
    time_period      VARCHAR(50)    NOT NULL,
    creation_date    TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_transaction_limit PRIMARY KEY (id),
    CONSTRAINT fk_transaction_limit_account FOREIGN KEY (account_id) REFERENCES account (id)
);
