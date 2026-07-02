package com.example.audit_service.repository;

import com.example.audit_service.entity.AuditRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecordEntity, Long>,
        JpaSpecificationExecutor<AuditRecordEntity> {

    Optional<AuditRecordEntity> findByAuditId(UUID auditId);

    boolean existsByEventId(UUID eventId);
}
