CREATE SEQUENCE notification_primary_key_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE notifications
(
    id                BIGINT                   NOT NULL DEFAULT nextval('notification_primary_key_seq'),
    notification_id   UUID                     NOT NULL,
    recipient_user_id UUID                     NOT NULL,
    type              VARCHAR(64)              NOT NULL,
    channel           VARCHAR(32)              NOT NULL,
    subject           VARCHAR(255),
    body              TEXT                     NOT NULL,
    status            VARCHAR(32)              NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at           TIMESTAMP WITH TIME ZONE,
    failure_reason    TEXT,
    version           BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT notifications_pkey PRIMARY KEY (id),
    CONSTRAINT notifications_notification_id_unique UNIQUE (notification_id),
    CONSTRAINT notifications_status_check CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT notifications_channel_check CHECK (channel IN ('EMAIL', 'IN_APP')),
    CONSTRAINT notifications_body_not_blank CHECK (char_length(btrim(body)) > 0),
    CONSTRAINT notifications_failure_reason_check CHECK (
        (status = 'FAILED' AND failure_reason IS NOT NULL AND char_length(btrim(failure_reason)) > 0)
        OR
        (status <> 'FAILED' AND failure_reason IS NULL)
    )
);

ALTER SEQUENCE notification_primary_key_seq OWNED BY notifications.id;

CREATE INDEX idx_notifications_recipient_user_id ON notifications (recipient_user_id);
CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_notifications_channel ON notifications (channel);
CREATE INDEX idx_notifications_created_at ON notifications (created_at);

CREATE SEQUENCE notification_preference_primary_key_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE notification_preferences
(
    id                BIGINT                   NOT NULL DEFAULT nextval('notification_preference_primary_key_seq'),
    user_id           UUID                     NOT NULL,
    email_enabled     BOOLEAN                  NOT NULL DEFAULT TRUE,
    in_app_enabled    BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    version           BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT notification_preferences_pkey PRIMARY KEY (id),
    CONSTRAINT notification_preferences_user_id_unique UNIQUE (user_id)
);

ALTER SEQUENCE notification_preference_primary_key_seq OWNED BY notification_preferences.id;
