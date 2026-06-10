package com.example.task_service.usecase.impl;

import com.example.task_service.dto.PageResponse;
import com.example.task_service.dto.TaskListQuery;
import com.example.task_service.dto.TaskListResponse;
import com.example.task_service.dto.TaskResponse;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.repository.TaskRepository;
import com.example.task_service.security.CurrentUserAccessProvider;
import com.example.task_service.security.CurrentUserAccessProvider.CurrentUserAccess;
import com.example.task_service.usecase.ListTasksUseCase;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListTasksUseCaseImpl implements ListTasksUseCase {

    private static final String DEFAULT_SORT = "createdAt,desc";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "createdAt",
        "updatedAt",
        "title",
        "status",
        "priority"
    );

    private final TaskRepository taskRepository;
    private final CurrentUserAccessProvider currentUserAccessProvider;

    public ListTasksUseCaseImpl(TaskRepository taskRepository, CurrentUserAccessProvider currentUserAccessProvider) {
        this.taskRepository = taskRepository;
        this.currentUserAccessProvider = currentUserAccessProvider;
    }

    @Override
    public TaskListResponse list(TaskListQuery query) {
        validate(query);

        CurrentUserAccess access = currentUserAccessProvider.currentUserAccess();
        Page<TaskEntity> page = taskRepository.findAll(toSpecification(query, access), toPageable(query));
        List<TaskResponse> items = page.getContent().stream()
            .map(this::toResponse)
            .toList();

        return new TaskListResponse(
            items,
            new PageResponse(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages())
        );
    }

    private void validate(TaskListQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Task list query is required");
        }
        if (query.getPage() < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }
        if (query.getSize() < 1 || query.getSize() > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }

    private Pageable toPageable(TaskListQuery query) {
        return PageRequest.of(query.getPage(), query.getSize(), toSort(query.getSort()));
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

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(directionValue);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported sort direction: " + directionValue);
        }
        return Sort.by(direction, field);
    }

    private Specification<TaskEntity> toSpecification(TaskListQuery query, CurrentUserAccess access) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!access.admin()) {
                predicates.add(visibleToUser(root, criteriaBuilder, access.userId()));
            }
            if (query.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), query.getStatus()));
            }
            if (query.getPriority() != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), query.getPriority()));
            }
            if (query.getAssigneeUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("assigneeUserId"), query.getAssigneeUserId()));
            }
            if (query.getCreatedByUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("createdByUserId"), query.getCreatedByUserId()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Predicate visibleToUser(Root<TaskEntity> root,
                                    CriteriaBuilder criteriaBuilder,
                                    UUID userId) {
        return criteriaBuilder.or(
            criteriaBuilder.equal(root.get("createdByUserId"), userId),
            criteriaBuilder.equal(root.get("assigneeUserId"), userId)
        );
    }

    private TaskResponse toResponse(TaskEntity task) {
        return new TaskResponse(
            task.getTaskId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getAssigneeUserId(),
            task.getCreatedByUserId(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
