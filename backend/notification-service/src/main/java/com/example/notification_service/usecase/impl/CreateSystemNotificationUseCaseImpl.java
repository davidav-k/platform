package com.example.notification_service.usecase.impl;

import com.example.notification_service.dto.CreateSystemNotificationRequest;
import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.mapper.NotificationMapper;
import com.example.notification_service.repository.NotificationRepository;
import com.example.notification_service.usecase.CreateSystemNotificationUseCase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateSystemNotificationUseCaseImpl implements CreateSystemNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final Validator validator;

    @Override
    public NotificationResponse create(CreateSystemNotificationRequest request) {
        validate(request);

        NotificationEntity notification = new NotificationEntity(
            null,
            request.recipientUserId(),
            request.type(),
            NotificationChannel.IN_APP,
            trimNullable(request.title()),
            request.message().trim(),
            NotificationStatus.PENDING,
            request.sourceService().trim(),
            request.sourceEntityType().trim(),
            request.sourceEntityId()
        );
        NotificationEntity savedNotification = notificationRepository.save(notification);
        log.info("Created system notification: notificationId={}, recipientUserId={}, type={}, sourceService={}, sourceEntityType={}, sourceEntityId={}",
            savedNotification.getNotificationId(),
            savedNotification.getRecipientUserId(),
            savedNotification.getType(),
            savedNotification.getSourceService(),
            savedNotification.getSourceEntityType(),
            savedNotification.getSourceEntityId());
        return NotificationMapper.toResponse(savedNotification);
    }

    private void validate(CreateSystemNotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create system notification request is required");
        }
        Set<ConstraintViolation<CreateSystemNotificationRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private String trimNullable(String value) {
        return value == null ? null : value.strip();
    }
}
