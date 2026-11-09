-- Status or email changes of a user consumed before the event that opens their account (each topic of
-- user-service is consumed on its own). They are applied, and deleted, when the account is opened.
CREATE TABLE pending_user_update (
    user_id    VARCHAR(255) NOT NULL,
    active     BOOLEAN,
    email      VARCHAR(255),
    updated_at TIMESTAMP,
    CONSTRAINT pk_pending_user_update PRIMARY KEY (user_id)
);
