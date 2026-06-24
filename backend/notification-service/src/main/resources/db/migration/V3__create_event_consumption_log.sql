CREATE SEQUENCE event_consumption_log_primary_key_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE event_consumption_log
(
    id          BIGINT                   NOT NULL DEFAULT nextval('event_consumption_log_primary_key_seq'),
    event_id    UUID                     NOT NULL,
    event_type  VARCHAR(100)             NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source      VARCHAR(100)             NOT NULL,
    CONSTRAINT event_consumption_log_pkey PRIMARY KEY (id),
    CONSTRAINT event_consumption_log_event_id_unique UNIQUE (event_id),
    CONSTRAINT event_consumption_log_event_type_not_blank CHECK (char_length(btrim(event_type)) > 0),
    CONSTRAINT event_consumption_log_source_not_blank CHECK (char_length(btrim(source)) > 0)
);

ALTER SEQUENCE event_consumption_log_primary_key_seq OWNED BY event_consumption_log.id;

CREATE INDEX idx_event_consumption_log_event_type ON event_consumption_log (event_type);
