CREATE TABLE account_balance (
    id                BIGSERIAL      NOT NULL,
    account_id        BIGINT         NOT NULL,
    balance           NUMERIC(19, 4) NOT NULL,
    balance_type      VARCHAR(50)    NOT NULL,
    creation_date     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_account_balance PRIMARY KEY (id),
    CONSTRAINT fk_account_balance_account FOREIGN KEY (account_id) REFERENCES account (id)
);
