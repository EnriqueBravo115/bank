CREATE TABLE users
(
    id                  BIGSERIAL    PRIMARY KEY,
    names               VARCHAR(255) NOT NULL UNIQUE,
    first_surname       VARCHAR(255) NOT NULL UNIQUE,
    second_surname      VARCHAR(255) NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(255) NOT NULL,
    rfc                 VARCHAR(255) NOT NULL,
    curp                VARCHAR(255) NOT NULL,
    country_of_birth    VARCHAR(255) NOT NULL,
    gender              VARCHAR(255) NOT NULL,
    role                VARCHAR(255) NOT NULL,
    phone_number        VARCHAR(255) NOT NULL UNIQUE,
    phone_code          VARCHAR(255),
    password_reset_code VARCHAR(255),
    country_code        VARCHAR(255),
    activation_code     VARCHAR(255),
    birthday            VARCHAR(255),
    active              BOOLEAN      NOT NULL DEFAULT FALSE,
    registration_date   TIMESTAMP    DEFAULT current_timestamp,
    updated_at          TIMESTAMP
);
