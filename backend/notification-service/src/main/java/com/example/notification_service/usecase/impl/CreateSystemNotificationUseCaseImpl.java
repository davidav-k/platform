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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class CreateSystemNotificationUseCaseImpl implements CreateSystemNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final Validator validator;

    public CreateSystemNotificationUseCaseImpl(NotificationRepository notificationRepository,
                                               Validator validator) {
        this.notificationRepository = notificationRepository;
        this.validator = validator;
    }

    @Override
    public NotificationResponse create(CreateSystemNotificationRequest request) {
        validate(request);

        NotificationEntity notification = new NotificationEntity(
            null,
            request.getRecipientUserId(),
            request.getType(),
            NotificationChannel.IN_APP,
            trimNullable(request.getTitle()),
            request.getMessage().trim(),
            NotificationStatus.PENDING,
            request.getSourceService().trim(),
            request.getSourceEntityType().trim(),
            request.getSourceEntityId()
        );
        return NotificationMapper.toResponse(notificationRepository.save(notification));
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
        return value == null ? null : value.trim();
    }
}
