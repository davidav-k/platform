package com.example.task_service.repository;

import com.example.task_service.entity.OutboxEventEntity;
import com.example.task_service.enumeration.OutboxEventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    Optional<OutboxEventEntity> findByEventId(UUID eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select event from OutboxEventEntity event
        where event.status in :statuses
          and event.retryCount < :maxRetries
        order by event.createdAt asc
        """)
    List<OutboxEventEntity> findClaimableEvents(
        @Param("statuses") Collection<OutboxEventStatus> statuses,
        @Param("maxRetries") int maxRetries,
        Pageable pageable
    );
}
