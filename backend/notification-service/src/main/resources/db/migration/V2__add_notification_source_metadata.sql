ALTER TABLE notifications
    ADD COLUMN source_service VARCHAR(100),
    ADD COLUMN source_entity_type VARCHAR(100),
    ADD COLUMN source_entity_id UUID;

CREATE INDEX idx_notifications_source_entity
    ON notifications (source_service, source_entity_type, source_entity_id);
