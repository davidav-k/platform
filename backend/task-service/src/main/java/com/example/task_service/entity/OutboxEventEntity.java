package com.example.task_service.entity;

import com.example.task_service.enumeration.OutboxEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Persistent outbox record for task-service integration events.
 *
 * <p>The row is intended to be written in the same local transaction as the
 * task aggregate change that produced it. A future task-service publisher will
 * read these records and deliver them asynchronously. This entity does not
 * publish events by itself and is not wired into task use cases yet.</p>
 */
@Getter
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "outbox_event_primary_key_generator")
    @SequenceGenerator(
        name = "outbox_event_primary_key_generator",
        sequenceName = "outbox_event_primary_key_seq",
        allocationSize = 1
    )
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false, unique = true)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboxEventStatus status;

    @Setter
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Setter
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Setter
    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(String aggregateType, UUID aggregateId, String eventType,
                             String payload, OutboxEventStatus status) {
        this(null, aggregateType, aggregateId, eventType, payload, status);
    }

    public OutboxEventEntity(UUID eventId, String aggregateType, UUID aggregateId, String eventType,
                             String payload, OutboxEventStatus status) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.retryCount = 0;
    }

    @PrePersist
    void beforeInsert() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
