CREATE TABLE users
(
    id                 SERIAL PRIMARY KEY,
    full_name          VARCHAR(255) NOT NULL,
    username           VARCHAR(255) NOT NULL UNIQUE,
    password           VARCHAR(255) NOT NULL,
    email              VARCHAR(255) NOT NULL UNIQUE,
    country            VARCHAR(255) NOT NULL,
    country_code       VARCHAR(255),
    activation_code    VARCHAR(255),
    gender             VARCHAR(15)  NOT NULL,
    phone_code         VARCHAR(255),
    birthday           VARCHAR(255),
    role               VARCHAR(255) NOT NULL,
    phone_number       INT8         NOT NULL UNIQUE,
    active             BOOLEAN      NOT NULL DEFAULT FALSE,
    registration_date  TIMESTAMP    DEFAULT current_timestamp,
    updated_at         TIMESTAMP
);
