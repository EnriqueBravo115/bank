-- One row per movement request, keyed by the transaction code that transaction-service sends as
-- Idempotency-Key: a retried request returns the stored result instead of moving money twice.
CREATE TABLE movement (
    id                    BIGSERIAL      NOT NULL,
    idempotency_key       VARCHAR(100)   NOT NULL,
    movement_type         VARCHAR(50),
    source_account_number VARCHAR(20),
    target_account_number VARCHAR(20),
    amount                NUMERIC(19, 4),
    status                VARCHAR(50)    NOT NULL,
    reason                VARCHAR(255),
    created_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP,
    CONSTRAINT pk_movement PRIMARY KEY (id),
    CONSTRAINT uk_movement_idempotency_key UNIQUE (idempotency_key)
);
