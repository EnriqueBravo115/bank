CREATE TABLE users
(
    id                  BIGSERIAL    PRIMARY KEY,
    active              BOOLEAN      NOT NULL DEFAULT FALSE,
    registration_date   TIMESTAMP    DEFAULT current_timestamp,
    updated_at          TIMESTAMP
);
