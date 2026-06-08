package com.example.notification_service.repository;

import com.example.notification_service.entity.NotificationPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, Long> {

    Optional<NotificationPreferenceEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
