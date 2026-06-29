package com.example.notification_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Durable record of an integration event already accepted by notification-service.
 *
 * <p>The event ID is the idempotency key for at-least-once Kafka delivery.
 * Persisting this row lets the consumer safely ignore duplicate deliveries
 * without creating duplicate notification side effects in later cutover work.</p>
 */
@Getter
@Entity
@Table(name = "event_consumption_log")
public class ConsumedEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_consumption_log_primary_key_generator")
    @SequenceGenerator(
            name = "event_consumption_log_primary_key_generator",
            sequenceName = "event_consumption_log_primary_key_seq",
            allocationSize = 1
    )
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "consumed_at", nullable = false, updatable = false)
    private OffsetDateTime consumedAt;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    protected ConsumedEventEntity() {
    }

    public ConsumedEventEntity(UUID eventId, String eventType, String source) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.source = source;
    }

    @PrePersist
    void beforeInsert() {
        if (consumedAt == null) {
            consumedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
