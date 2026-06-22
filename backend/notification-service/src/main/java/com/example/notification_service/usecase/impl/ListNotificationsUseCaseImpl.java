package com.example.notification_service.usecase.impl;

import com.example.notification_service.dto.NotificationListQuery;
import com.example.notification_service.dto.NotificationListResponse;
import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.dto.PageResponse;
import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.mapper.NotificationMapper;
import com.example.notification_service.repository.NotificationRepository;
import com.example.notification_service.usecase.ListNotificationsUseCase;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lists notifications with validated filtering, pagination, and sorting.
 */
@Service
@Transactional(readOnly = true)
public class ListNotificationsUseCaseImpl implements ListNotificationsUseCase {

    private static final String DEFAULT_SORT = "createdAt,desc";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "status", "channel", "type"
    );

    private final NotificationRepository notificationRepository;

    public ListNotificationsUseCaseImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public NotificationListResponse list(NotificationListQuery query) {
        validate(query);

        Page<NotificationEntity> page = notificationRepository.findAll(toSpecification(query), toPageable(query));
        List<NotificationResponse> items = page.getContent().stream()
                .map(NotificationMapper::toResponse)
                .toList();

        return new NotificationListResponse(
                items,
                new PageResponse(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages())
        );
    }

    private void validate(NotificationListQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Notification list query is required");
        }
        if (query.page() < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }
        if (query.size() < 1 || query.size() > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }

    private Pageable toPageable(NotificationListQuery query) {
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
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported sort direction: " + directionValue);
        }
    }

    private Specification<NotificationEntity> toSpecification(NotificationListQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.recipientUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("recipientUserId"), query.recipientUserId()));
            }
            if (query.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), query.status()));
            }
            if (query.channel() != null) {
                predicates.add(criteriaBuilder.equal(root.get("channel"), query.channel()));
            }
            if (query.type() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), query.type()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
