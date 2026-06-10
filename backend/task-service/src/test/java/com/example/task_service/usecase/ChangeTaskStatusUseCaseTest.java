package com.example.task_service.usecase;

import com.example.task_service.dto.TaskResponse;
import com.example.task_service.dto.UpdateTaskStatusRequest;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:",
    "eureka.client.enabled=false",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:change_task_status_use_case_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ChangeTaskStatusUseCaseTest {

    @Autowired
    private ChangeTaskStatusUseCase changeTaskStatusUseCase;

    @Autowired
    private TaskRepository taskRepository;

    @MockitoBean
    private CurrentUserAccessProvider currentUserAccessProvider;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void adminChangesStatusOfAnyTask() {
        TaskEntity task = saveTask(UUID.randomUUID(), UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(UUID.randomUUID(), true));

        TaskResponse response = changeTaskStatusUseCase.changeStatus(
            task.getTaskId(),
            new UpdateTaskStatusRequest(TaskStatus.DONE)
        );

        assertThat(response.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void creatorChangesStatusOfOwnTask() {
        UUID creatorUserId = UUID.randomUUID();
        TaskEntity task = saveTask(UUID.randomUUID(), creatorUserId);
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));

        TaskResponse response = changeTaskStatusUseCase.changeStatus(
            task.getTaskId(),
            new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS)
        );

        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void assigneeChangesStatusOfAssignedTask() {
        UUID assigneeUserId = UUID.randomUUID();
        TaskEntity task = saveTask(assigneeUserId, UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(assigneeUserId, false));

        TaskResponse response = changeTaskStatusUseCase.changeStatus(
            task.getTaskId(),
            new UpdateTaskStatusRequest(TaskStatus.CANCELLED)
        );

        assertThat(response.getStatus()).isEqualTo(TaskStatus.CANCELLED);
    }

    @Test
    void unrelatedUserCannotChangeStatus() {
        TaskEntity task = saveTask(UUID.randomUUID(), UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(UUID.randomUUID(), false));

        assertThatThrownBy(() -> changeTaskStatusUseCase.changeStatus(
            task.getTaskId(),
            new UpdateTaskStatusRequest(TaskStatus.DONE)
        )).isInstanceOf(TaskNotFoundException.class);

        assertThat(taskRepository.findByTaskId(task.getTaskId()).orElseThrow().getStatus())
            .isEqualTo(TaskStatus.NEW);
    }

    @Test
    void statusChangeUpdatesOnlyStatusAndAuditField() {
        UUID assigneeUserId = UUID.randomUUID();
        UUID creatorUserId = UUID.randomUUID();
        TaskEntity task = saveTask(assigneeUserId, creatorUserId);
        UUID taskId = task.getTaskId();
        String title = task.getTitle();
        String description = task.getDescription();
        TaskPriority priority = task.getPriority();
        OffsetDateTime createdAt = task.getCreatedAt();
        OffsetDateTime originalUpdatedAt = task.getUpdatedAt();
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));

        changeTaskStatusUseCase.changeStatus(
            taskId,
            new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS)
        );

        TaskEntity updated = taskRepository.findByTaskId(taskId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updated.getTaskId()).isEqualTo(taskId);
        assertThat(updated.getTitle()).isEqualTo(title);
        assertThat(updated.getDescription()).isEqualTo(description);
        assertThat(updated.getPriority()).isEqualTo(priority);
        assertThat(updated.getAssigneeUserId()).isEqualTo(assigneeUserId);
        assertThat(updated.getCreatedByUserId()).isEqualTo(creatorUserId);
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void nullStatusIsRejected() {
        assertThatThrownBy(() -> changeTaskStatusUseCase.changeStatus(
            UUID.randomUUID(),
            new UpdateTaskStatusRequest(null)
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Status is required");
    }

    private TaskEntity saveTask(UUID assigneeUserId, UUID createdByUserId) {
        return taskRepository.saveAndFlush(new TaskEntity(
            UUID.randomUUID(),
            "Original title",
            "Original description",
            TaskStatus.NEW,
            TaskPriority.MEDIUM,
            assigneeUserId,
            createdByUserId
        ));
    }
}
