package com.example.audit_service.service.impl;

import com.example.audit_service.dto.AuditListQuery;
import com.example.audit_service.dto.AuditListResponse;
import com.example.audit_service.dto.AuditResponseDto;
import com.example.audit_service.dto.PageResponse;
import com.example.audit_service.entity.AuditRecordEntity;
import com.example.audit_service.exception.AuditRecordNotFoundException;
import com.example.audit_service.mapper.AuditMapper;
import com.example.audit_service.repository.AuditRecordRepository;
import com.example.audit_service.service.AuditQueryService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditQueryServiceImpl implements AuditQueryService {

    private static final String DEFAULT_SORT = "occurredAt,desc";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "auditId",
            "eventId",
            "eventType",
            "aggregateType",
            "aggregateId",
            "sourceService",
            "actorUserId",
            "actorEmail",
            "action",
            "occurredAt",
            "createdAt"
    );

    private final AuditRecordRepository auditRecordRepository;

    @Override
    public AuditListResponse findAll(AuditListQuery query) {
        validate(query);

        Page<AuditRecordEntity> page = auditRecordRepository.findAll(
                toSpecification(query),
                toPageable(query)
        );
        List<AuditResponseDto> items = page.getContent().stream()
                .map(AuditMapper::toResponse)
                .toList();

        return new AuditListResponse(
                items,
                new PageResponse(page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())
        );
    }

    @Override
    public AuditResponseDto getByAuditId(UUID auditId) {
        if (auditId == null) {
            throw new IllegalArgumentException("Audit ID is required");
        }
        return auditRecordRepository.findByAuditId(auditId)
                .map(AuditMapper::toResponse)
                .orElseThrow(() -> new AuditRecordNotFoundException(auditId));
    }

    private void validate(AuditListQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Audit list query is required");
        }
        if (query.page() < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }
        if (query.size() < 1 || query.size() > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw new IllegalArgumentException("From must be before or equal to to");
        }
    }

    private Pageable toPageable(AuditListQuery query) {
        return PageRequest.of(query.page(), query.size(), toSort(query.sort()));
    }

    private Sort toSort(String sortValue) {
        String value = sortValue == null || sortValue.isBlank() ? DEFAULT_SORT : sortValue;
        String[] parts = value.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Sort must use the format field,direction");
        }

        String field = parts[0].trim();
        String directionValue = parts[1].trim();
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Unsupported sort field: " + field);
        }

        try {
            return Sort.by(Sort.Direction.fromString(directionValue), field);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported sort direction: " + directionValue);
        }
    }

    private Specification<AuditRecordEntity> toSpecification(AuditListQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.eventType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("eventType"), query.eventType()));
            }
            if (query.aggregateType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("aggregateType"), query.aggregateType()));
            }
            if (query.aggregateId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("aggregateId"), query.aggregateId()));
            }
            if (query.sourceService() != null) {
                predicates.add(criteriaBuilder.equal(root.get("sourceService"), query.sourceService()));
            }
            if (query.actorUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorUserId"), query.actorUserId()));
            }
            if (query.action() != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), query.action()));
            }
            if (query.from() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.<OffsetDateTime>get("occurredAt"), query.from()));
            }
            if (query.to() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.<OffsetDateTime>get("occurredAt"), query.to()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
