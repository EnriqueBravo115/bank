CREATE TABLE user_kyc
(
    id               BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    rfc              VARCHAR(13)  NOT NULL UNIQUE,
    curp             VARCHAR(18)  NOT NULL UNIQUE,
    document_type    VARCHAR(50),
    updated_at       TIMESTAMP
);
