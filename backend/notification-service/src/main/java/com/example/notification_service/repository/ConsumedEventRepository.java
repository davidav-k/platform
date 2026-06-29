package com.example.notification_service.repository;

import com.example.notification_service.entity.ConsumedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsumedEventRepository extends JpaRepository<ConsumedEventEntity, Long> {

    boolean existsByEventId(UUID eventId);

    Optional<ConsumedEventEntity> findByEventId(UUID eventId);
}
