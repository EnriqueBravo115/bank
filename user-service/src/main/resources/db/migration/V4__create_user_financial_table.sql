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
