CREATE TABLE card
(
    id                SERIAL PRIMARY KEY,
    card_number       INT NOT NULL,
    card_holder_name  VARCHAR(255),
    cvv               VARCHAR(255),
    card_type         VARCHAR(255),
    creation_date     TIMESTAMP DEFAULT current_timestamp,
    status            VARCHAR(255),
    account_id        INT REFERENCES account(id) ON DELETE SET NULL
);
