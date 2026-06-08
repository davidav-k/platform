package com.example.notification_service.usecase.impl;

import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.exception.NotificationNotFoundException;
import com.example.notification_service.mapper.NotificationMapper;
import com.example.notification_service.repository.NotificationRepository;
import com.example.notification_service.usecase.GetNotificationUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Retrieves one notification by its public identifier.
 */
@Service
@Transactional(readOnly = true)
public class GetNotificationUseCaseImpl implements GetNotificationUseCase {

    private final NotificationRepository notificationRepository;

    public GetNotificationUseCaseImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public NotificationResponse getByNotificationId(UUID notificationId) {
        if (notificationId == null) {
            throw new IllegalArgumentException("Notification ID is required");
        }
        return notificationRepository.findByNotificationId(notificationId)
                .map(NotificationMapper::toResponse)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
    }
}
