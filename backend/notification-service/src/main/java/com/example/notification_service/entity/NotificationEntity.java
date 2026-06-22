package com.example.notification_service.entity;

import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;
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


@Getter
@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_primary_key_generator")
    @SequenceGenerator(
            name = "notification_primary_key_generator",
            sequenceName = "notification_primary_key_seq",
            allocationSize = 1
    )
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "notification_id", nullable = false, updatable = false, unique = true)
    private UUID notificationId;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private NotificationChannel channel;

    @Column(name = "subject", length = 255)
    private String subject;

    @Setter
    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Setter
    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "source_service", length = 100)
    private String sourceService;

    @Column(name = "source_entity_type", length = 100)
    private String sourceEntityType;

    @Column(name = "source_entity_id")
    private UUID sourceEntityId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected NotificationEntity() {
    }

    public NotificationEntity(UUID notificationId, UUID recipientUserId, NotificationType type,
                              NotificationChannel channel, String subject, String body,
                              NotificationStatus status) {
        this(notificationId, recipientUserId, type, channel, subject, body, status, null, null, null);
    }

    public NotificationEntity(UUID notificationId, UUID recipientUserId, NotificationType type,
                              NotificationChannel channel, String subject, String body,
                              NotificationStatus status, String sourceService,
                              String sourceEntityType, UUID sourceEntityId) {
        this.notificationId = notificationId;
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.sourceService = sourceService;
        this.sourceEntityType = sourceEntityType;
        this.sourceEntityId = sourceEntityId;
    }

    @PrePersist
    void beforeInsert() {
        if (notificationId == null) {
            notificationId = UUID.randomUUID();
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
