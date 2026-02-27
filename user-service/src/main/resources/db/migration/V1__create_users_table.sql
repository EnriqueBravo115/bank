CREATE TABLE users
(
    id                  BIGSERIAL    PRIMARY KEY,
    keycloak_id         VARCHAR(255) NOT NULL UNIQUE,
    names               VARCHAR(255) NOT NULL,
    first_surname       VARCHAR(255) NOT NULL,
    second_surname      VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    rfc                 VARCHAR(13)  NOT NULL UNIQUE,
    curp                VARCHAR(18)  NOT NULL UNIQUE,
    country_of_birth    VARCHAR(2)   NOT NULL,
    gender              VARCHAR(50)  NOT NULL,
    role                VARCHAR(50)  NOT NULL,
    phone_number        VARCHAR(20)  NOT NULL UNIQUE,
    phone_code          VARCHAR(10),
    password_reset_code VARCHAR(255),
    activation_code     VARCHAR(255),
    birthday            VARCHAR(10),
    active              BOOLEAN      NOT NULL DEFAULT FALSE,
    registration_date   TIMESTAMP    DEFAULT current_timestamp,
    updated_at          TIMESTAMP
);
