package com.example.notification_service.usecase.impl;

import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.mapper.NotificationMapper;
import com.example.notification_service.outbox.NotificationOutboxEventTypes;
import com.example.notification_service.outbox.NotificationOutboxPayloadFactory;
import com.example.notification_service.outbox.OutboxEventService;
import com.example.notification_service.repository.NotificationRepository;
import com.example.notification_service.usecase.CreateNotificationUseCase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Creates pending notification records without performing delivery.
 */
@Service
@RequiredArgsConstructor
public class CreateNotificationUseCaseImpl implements CreateNotificationUseCase {

    private static final String NOTIFICATION_AGGREGATE_TYPE = "NOTIFICATION";

    private final NotificationRepository notificationRepository;
    private final Validator validator;
    private final OutboxEventService outboxEventService;
    private final NotificationOutboxPayloadFactory notificationOutboxPayloadFactory;


    @Override
    @Transactional
    public NotificationResponse create(CreateNotificationRequest request) {
        validate(request);

        NotificationEntity notification = new NotificationEntity(
                null,
                request.recipientUserId(),
                request.type(),
                request.channel(),
                trimNullable(request.subject()),
                request.body().trim(),
                NotificationStatus.PENDING
        );

        NotificationEntity savedNotification = notificationRepository.save(notification);
        outboxEventService.saveNewEvent(
                NOTIFICATION_AGGREGATE_TYPE,
                savedNotification.getNotificationId(),
                NotificationOutboxEventTypes.NOTIFICATION_CREATED,
                notificationOutboxPayloadFactory.notificationCreatedPayload(savedNotification)
        );
        return NotificationMapper.toResponse(savedNotification);
    }

    private void validate(CreateNotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create notification request is required");
        }
        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private String trimNullable(String value) {
        return value == null ? null : value.strip();
    }
}
