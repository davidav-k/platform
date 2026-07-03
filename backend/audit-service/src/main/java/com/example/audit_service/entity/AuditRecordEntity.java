package com.example.audit_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Persistent audit record created from an integration event.
 *
 * <p>The source event ID is the idempotency key. The original event payload is
 * retained as JSONB so later audit projections do not lose source data.</p>
 */
@Getter
@Entity
@Table(name = "audit_records")
public class AuditRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_record_primary_key_generator")
    @SequenceGenerator(
            name = "audit_record_primary_key_generator",
            sequenceName = "audit_record_primary_key_seq",
            allocationSize = 1
    )
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "audit_id", nullable = false, updatable = false, unique = true)
    private UUID auditId;

    @Column(name = "event_id", nullable = false, updatable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_email", length = 320)
    private String actorEmail;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected AuditRecordEntity() {
    }

    public AuditRecordEntity(UUID auditId, UUID eventId, String eventType, String aggregateType,
                             UUID aggregateId, String sourceService, UUID actorUserId,
                             String actorEmail, String action, String payload,
                             OffsetDateTime occurredAt) {
        this.auditId = auditId;
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.sourceService = sourceService;
        this.actorUserId = actorUserId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    @PrePersist
    void beforeInsert() {
        if (auditId == null) {
            auditId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

}
