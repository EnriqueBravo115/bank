CREATE TABLE account (
    id              BIGSERIAL       NOT NULL,
    user_id         VARCHAR(255)    NOT NULL,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    clabe           VARCHAR(18)     NOT NULL UNIQUE,
    account_number  VARCHAR(20)     NOT NULL,
    account_type    VARCHAR(50)     NOT NULL,
    account_status  VARCHAR(50)     NOT NULL,
    currency        VARCHAR(10)     NOT NULL,
    creation_date   TIMESTAMP       NOT NULL,
    CONSTRAINT pk_account PRIMARY KEY (id)
);
