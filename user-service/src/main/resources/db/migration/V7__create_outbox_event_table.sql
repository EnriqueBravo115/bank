-- Transactional outbox: events are stored in the same transaction as the business change
-- and published to Kafka afterwards by the outbox publisher.
CREATE TABLE outbox_event
(
    id           UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    topic        VARCHAR(255) NOT NULL,
    payload      JSONB        NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    attempts     INT          NOT NULL DEFAULT 0,
    last_error   VARCHAR(500),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at      TIMESTAMP
);

CREATE INDEX idx_outbox_event_pending ON outbox_event (created_at) WHERE status = 'PENDING';
