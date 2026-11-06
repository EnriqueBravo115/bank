-- Backoff between attempts: a failed event waits until next_attempt_at before being sent again.
ALTER TABLE outbox_event ADD COLUMN next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Publishing checks for earlier unsent events of the same user (per-user ordering).
CREATE INDEX idx_outbox_event_unsent_by_aggregate ON outbox_event (aggregate_id, created_at) WHERE status <> 'SENT';

-- Admin listing of FAILED events and the cleanup of old SENT ones.
CREATE INDEX idx_outbox_event_failed ON outbox_event (created_at) WHERE status = 'FAILED';
CREATE INDEX idx_outbox_event_sent ON outbox_event (sent_at) WHERE status = 'SENT';
