CREATE TABLE transactions
(
    id                 BIGSERIAL PRIMARY KEY,
    source_identifier  VARCHAR(255),
    identifier_type    VARCHAR(255),
    transaction_code   VARCHAR(255),
    amount             NUMERIC(19, 2),
    description        TEXT,
    reason             TEXT,
    transaction_date   TIMESTAMP,
    currency           VARCHAR(50),
    transaction_type   VARCHAR(50),
    transaction_status VARCHAR(50),
    transfer_id        BIGINT UNIQUE,
    purchase_id        BIGINT UNIQUE,
    service_id         BIGINT UNIQUE,
    withdrawal_id      BIGINT UNIQUE,
    CONSTRAINT fk_transfer FOREIGN KEY (transfer_id) REFERENCES transfer_transaction (id),
    CONSTRAINT fk_purchase FOREIGN KEY (purchase_id) REFERENCES purchase_transaction (id),
    CONSTRAINT fk_service FOREIGN KEY (service_id) REFERENCES service_transaction (id),
    CONSTRAINT fk_withdrawal FOREIGN KEY (withdrawal_id) REFERENCES withdrawal_transaction (id)
);

CREATE TABLE transfer_transaction
(
    id                    BIGSERIAL PRIMARY KEY,
    source_account_number VARCHAR(255),
    target_account_number VARCHAR(255)
);

CREATE TABLE purchase_transaction
(
    id                 BIGSERIAL PRIMARY KEY,
    source_card_number VARCHAR(255),
    cvv                VARCHAR(10),
    merchant_code      VARCHAR(50),
    merchant_category  VARCHAR(100),
    pos_id             VARCHAR(50)
);

CREATE TABLE service_transaction
(
    id                    BIGSERIAL PRIMARY KEY,
    source_account_number VARCHAR(255),
    payment_reference     VARCHAR(255),
    service_type          VARCHAR(50)
);

CREATE TABLE withdrawal_transaction
(
    id                           BIGSERIAL PRIMARY KEY,
    atm_location                 VARCHAR(255),
    atm_session_id               VARCHAR(255),
    receipt_number               VARCHAR(255),
    branch_code                  VARCHAR(50),
    teller_id                    VARCHAR(50),
    transaction_fee              NUMERIC(19, 2),
    security_verification_method VARCHAR(50),
    withdrawal_method            VARCHAR(50)
);
