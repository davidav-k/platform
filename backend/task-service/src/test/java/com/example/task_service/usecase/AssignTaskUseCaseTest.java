package com.example.task_service.usecase;

import com.example.task_service.dto.AssignTaskRequest;
import com.example.task_service.dto.TaskResponse;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.exception.TaskNotFoundException;
import com.example.task_service.repository.TaskRepository;
import com.example.task_service.security.CurrentUserAccessProvider;
import com.example.task_service.security.CurrentUserAccessProvider.CurrentUserAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:",
    "eureka.client.enabled=false",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:assign_task_use_case_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AssignTaskUseCaseTest {

    @Autowired
    private AssignTaskUseCase assignTaskUseCase;

    @Autowired
    private TaskRepository taskRepository;

    @MockitoBean
    private CurrentUserAccessProvider currentUserAccessProvider;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void adminAssignsAnyTask() {
        TaskEntity task = saveTask(UUID.randomUUID(), UUID.randomUUID());
        UUID newAssigneeUserId = UUID.randomUUID();
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(UUID.randomUUID(), true));

        TaskResponse response = assignTaskUseCase.assign(
            task.getTaskId(),
            new AssignTaskRequest(newAssigneeUserId)
        );

        assertThat(response.getAssigneeUserId()).isEqualTo(newAssigneeUserId);
    }

    @Test
    void creatorAssignsOwnTask() {
        UUID creatorUserId = UUID.randomUUID();
        TaskEntity task = saveTask(null, creatorUserId);
        UUID newAssigneeUserId = UUID.randomUUID();
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));

        TaskResponse response = assignTaskUseCase.assign(
            task.getTaskId(),
            new AssignTaskRequest(newAssigneeUserId)
        );

        assertThat(response.getAssigneeUserId()).isEqualTo(newAssigneeUserId);
    }

    @Test
    void creatorCanUnassignTask() {
        UUID creatorUserId = UUID.randomUUID();
        TaskEntity task = saveTask(UUID.randomUUID(), creatorUserId);
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));

        TaskResponse response = assignTaskUseCase.assign(
            task.getTaskId(),
            new AssignTaskRequest(null)
        );

        assertThat(response.getAssigneeUserId()).isNull();
    }

    @Test
    void assigneeCannotReassignTaskCreatedByAnotherUser() {
        UUID assigneeUserId = UUID.randomUUID();
        TaskEntity task = saveTask(assigneeUserId, UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(assigneeUserId, false));

        assertThatThrownBy(() -> assignTaskUseCase.assign(
            task.getTaskId(),
            new AssignTaskRequest(UUID.randomUUID())
        )).isInstanceOf(TaskNotFoundException.class);

        assertThat(taskRepository.findByTaskId(task.getTaskId()).orElseThrow().getAssigneeUserId())
            .isEqualTo(assigneeUserId);
    }

    @Test
    void unrelatedUserCannotAssignTask() {
        TaskEntity task = saveTask(null, UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(UUID.randomUUID(), false));

        assertThatThrownBy(() -> assignTaskUseCase.assign(
            task.getTaskId(),
            new AssignTaskRequest(UUID.randomUUID())
        )).isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void softDeletedTaskCannotBeAssigned() {
        UUID creatorUserId = UUID.randomUUID();
        TaskEntity task = saveTask(null, creatorUserId);
        task.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        task.setDeletedByUserId(creatorUserId);
        taskRepository.saveAndFlush(task);
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));

        assertThatThrownBy(() -> assignTaskUseCase.assign(
            task.getTaskId(),
            new AssignTaskRequest(UUID.randomUUID())
        )).isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void assignmentUpdatesOnlyAssigneeAndAuditField() {
        UUID creatorUserId = UUID.randomUUID();
        TaskEntity task = saveTask(null, creatorUserId);
        UUID taskId = task.getTaskId();
        OffsetDateTime originalUpdatedAt = task.getUpdatedAt();
        UUID newAssigneeUserId = UUID.randomUUID();
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));

        assignTaskUseCase.assign(taskId, new AssignTaskRequest(newAssigneeUserId));

        TaskEntity updated = taskRepository.findByTaskId(taskId).orElseThrow();
        assertThat(updated.getAssigneeUserId()).isEqualTo(newAssigneeUserId);
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updated.getTitle()).isEqualTo("Assignment task");
        assertThat(updated.getDescription()).isEqualTo("Existing description");
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(updated.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(updated.getCreatedByUserId()).isEqualTo(creatorUserId);
        assertThat(updated.getDeletedAt()).isNull();
    }

    @Test
    void omittedAssigneeUserIdIsRejected() {
        assertThatThrownBy(() -> assignTaskUseCase.assign(
            UUID.randomUUID(),
            new AssignTaskRequest()
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Assignee user ID must be provided");
    }

    private TaskEntity saveTask(UUID assigneeUserId, UUID createdByUserId) {
        return taskRepository.saveAndFlush(new TaskEntity(
            UUID.randomUUID(),
            "Assignment task",
            "Existing description",
            TaskStatus.NEW,
            TaskPriority.MEDIUM,
            assigneeUserId,
            createdByUserId
        ));
    }
}
