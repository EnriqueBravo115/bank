CREATE TABLE card (
    id               BIGSERIAL    NOT NULL,
    account_id       BIGINT       NOT NULL,
    card_number      VARCHAR(16)  NOT NULL UNIQUE,
    card_holder_name VARCHAR(255) NOT NULL,
    cvv              VARCHAR(255) NOT NULL,
    card_type        VARCHAR(50)  NOT NULL,
    card_status      VARCHAR(50)  NOT NULL,
    expiration_date  DATE         NOT NULL,
    creation_date    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_card PRIMARY KEY (id),
    CONSTRAINT fk_card_account FOREIGN KEY (account_id) REFERENCES account (id)
);
