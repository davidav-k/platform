CREATE SEQUENCE outbox_event_primary_key_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE outbox_events
(
    id             BIGINT                   NOT NULL DEFAULT nextval('outbox_event_primary_key_seq'),
    event_id       UUID                     NOT NULL,
    aggregate_type VARCHAR(100)             NOT NULL,
    aggregate_id   UUID                     NOT NULL,
    event_type     VARCHAR(100)             NOT NULL,
    payload        JSONB                    NOT NULL,
    status         VARCHAR(32)              NOT NULL,
    retry_count    INTEGER                  NOT NULL DEFAULT 0,
    error_message  TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at   TIMESTAMP WITH TIME ZONE,
    version        BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT outbox_events_pkey PRIMARY KEY (id),
    CONSTRAINT outbox_events_event_id_unique UNIQUE (event_id),
    CONSTRAINT outbox_events_status_check CHECK (status IN ('NEW', 'PROCESSING', 'PROCESSED', 'FAILED')),
    CONSTRAINT outbox_events_retry_count_non_negative CHECK (retry_count >= 0),
    CONSTRAINT outbox_events_aggregate_type_not_blank CHECK (char_length(btrim(aggregate_type)) > 0),
    CONSTRAINT outbox_events_event_type_not_blank CHECK (char_length(btrim(event_type)) > 0)
);

ALTER SEQUENCE outbox_event_primary_key_seq OWNED BY outbox_events.id;

CREATE INDEX idx_outbox_events_status_created_at ON outbox_events (status, created_at);
CREATE INDEX idx_outbox_events_aggregate ON outbox_events (aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_events_event_type ON outbox_events (event_type);
