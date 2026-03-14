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
