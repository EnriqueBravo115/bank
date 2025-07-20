CREATE TABLE users
(
    id                  BIGSERIAL    PRIMARY KEY,
    username            VARCHAR(255) NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    full_name           VARCHAR(255) NOT NULL,
    password            VARCHAR(255) NOT NULL,
    country             VARCHAR(255) NOT NULL,
    gender              VARCHAR(255) NOT NULL,
    role                VARCHAR(255) NOT NULL,
    phone_number        VARCHAR(255) NOT NULL UNIQUE,
    phone_code          VARCHAR(255),
    password_reset_code VARCHAR(255),
    country_code        VARCHAR(255),
    activation_code     VARCHAR(255),
    birthday            DATE,
    active              BOOLEAN      NOT NULL DEFAULT FALSE,
    registration_date   TIMESTAMP    DEFAULT current_timestamp,
    updated_at          TIMESTAMP
);
