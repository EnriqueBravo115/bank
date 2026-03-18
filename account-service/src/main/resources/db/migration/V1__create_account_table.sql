CREATE TABLE account (
    id              BIGSERIAL       NOT NULL,
    user_id         BIGINT          NOT NULL,
    clabe           VARCHAR(18)     NULL,
    account_number  VARCHAR(20)     NOT NULL,
    account_type    VARCHAR(50)     NOT NULL,
    account_status  VARCHAR(50)     NOT NULL,
    currency        VARCHAR(10)     NOT NULL,
    minimum_balance NUMERIC(19, 4)  NULL,
    creation_date   TIMESTAMP       NOT NULL,
    CONSTRAINT pk_account PRIMARY KEY (id),
    CONSTRAINT uq_account_clabe          UNIQUE (clabe),
    CONSTRAINT uq_account_account_number UNIQUE (account_number)
);
