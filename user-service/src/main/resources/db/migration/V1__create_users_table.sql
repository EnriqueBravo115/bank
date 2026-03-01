CREATE TABLE users
(
    id              BIGSERIAL PRIMARY KEY,
    keycloak_id     VARCHAR(255) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone_number    VARCHAR(20)  NOT NULL UNIQUE,
    phone_code      VARCHAR(10),
    role            VARCHAR(50)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT FALSE,
    register_status VARCHAR(255) NOT NULL,
    registration_date TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE TABLE user_profiles
(
    id               BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    names            VARCHAR(255) NOT NULL,
    first_surname    VARCHAR(255) NOT NULL,
    second_surname   VARCHAR(255) NOT NULL,
    birthday         VARCHAR(10),
    gender           VARCHAR(50)  NOT NULL,
    country_of_birth VARCHAR(2)   NOT NULL,
    updated_at       TIMESTAMP
);

CREATE TABLE user_kyc
(
    id               BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    rfc              VARCHAR(13)  NOT NULL UNIQUE,
    curp             VARCHAR(18)  NOT NULL UNIQUE,
    document_type    VARCHAR(50),
    updated_at       TIMESTAMP
);

CREATE TABLE user_financial_info
(
    id               BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    occupation_type  VARCHAR(255),
    employer_name    VARCHAR(255),
    income_source    VARCHAR(255),
    marital_status   VARCHAR(255),
    monthly_income   DECIMAL(12, 2),
    updated_at       TIMESTAMP
);

CREATE INDEX idx_users_email        ON users(email);
CREATE INDEX idx_users_phone_number ON users(phone_number);
CREATE INDEX idx_users_keycloak_id  ON users(keycloak_id);

CREATE INDEX idx_kyc_rfc            ON user_kyc(rfc);
CREATE INDEX idx_kyc_curp           ON user_kyc(curp);
