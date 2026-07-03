CREATE SEQUENCE audit_record_primary_key_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE audit_records
(
    id             BIGINT                   NOT NULL DEFAULT nextval('audit_record_primary_key_seq'),
    audit_id       UUID                     NOT NULL,
    event_id       UUID                     NOT NULL,
    event_type     VARCHAR(100)             NOT NULL,
    aggregate_type VARCHAR(100)             NOT NULL,
    aggregate_id   UUID                     NOT NULL,
    source_service VARCHAR(100)             NOT NULL,
    actor_user_id  UUID,
    actor_email    VARCHAR(320),
    action         VARCHAR(100)             NOT NULL,
    payload        JSONB                    NOT NULL,
    occurred_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    version        BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT audit_records_pkey PRIMARY KEY (id),
    CONSTRAINT audit_records_audit_id_unique UNIQUE (audit_id),
    CONSTRAINT audit_records_event_id_unique UNIQUE (event_id),
    CONSTRAINT audit_records_event_type_not_blank CHECK (char_length(btrim(event_type)) > 0),
    CONSTRAINT audit_records_aggregate_type_not_blank CHECK (char_length(btrim(aggregate_type)) > 0),
    CONSTRAINT audit_records_source_service_not_blank CHECK (char_length(btrim(source_service)) > 0),
    CONSTRAINT audit_records_actor_email_not_blank CHECK (actor_email IS NULL OR char_length(btrim(actor_email)) > 0),
    CONSTRAINT audit_records_action_not_blank CHECK (char_length(btrim(action)) > 0)
);

ALTER SEQUENCE audit_record_primary_key_seq OWNED BY audit_records.id;

CREATE INDEX idx_audit_records_event_type ON audit_records (event_type);
CREATE INDEX idx_audit_records_aggregate ON audit_records (aggregate_type, aggregate_id);
CREATE INDEX idx_audit_records_source_service ON audit_records (source_service);
CREATE INDEX idx_audit_records_actor_user_id ON audit_records (actor_user_id);
CREATE INDEX idx_audit_records_occurred_at ON audit_records (occurred_at);
CREATE INDEX idx_audit_records_created_at ON audit_records (created_at);
