package com.example.task_service.usecase;

import com.example.task_service.dto.TaskResponse;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.exception.TaskNotFoundException;
import com.example.task_service.repository.TaskRepository;
import com.example.task_service.security.CurrentUserAccessProvider;
import com.example.task_service.security.CurrentUserAccessProvider.CurrentUserAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:",
    "eureka.client.enabled=false",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:get_task_use_case_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class GetTaskUseCaseTest {

    @Autowired
    private GetTaskUseCase getTaskUseCase;

    @Autowired
    private TaskRepository taskRepository;

    @MockitoBean
    private CurrentUserAccessProvider currentUserAccessProvider;

    @Test
    void returnsTaskByPublicTaskId() {
        UUID taskId = UUID.randomUUID();
        UUID assigneeUserId = UUID.randomUUID();
        UUID createdByUserId = UUID.randomUUID();
        taskRepository.saveAndFlush(new TaskEntity(
            taskId,
            "Read task",
            "Return one task by public id.",
            TaskStatus.IN_PROGRESS,
            TaskPriority.HIGH,
            assigneeUserId,
            createdByUserId
        ));
        when(currentUserAccessProvider.currentUserAccess()).thenReturn(new CurrentUserAccess(UUID.randomUUID(), true));

        TaskResponse response = getTaskUseCase.getByTaskId(taskId);

        assertThat(response.getTaskId()).isEqualTo(taskId);
        assertThat(response.getTitle()).isEqualTo("Read task");
        assertThat(response.getDescription()).isEqualTo("Return one task by public id.");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.getAssigneeUserId()).isEqualTo(assigneeUserId);
        assertThat(response.getCreatedByUserId()).isEqualTo(createdByUserId);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void creatorCanReadTask() {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        saveTask(taskId, UUID.randomUUID(), userId);
        when(currentUserAccessProvider.currentUserAccess()).thenReturn(new CurrentUserAccess(userId, false));

        assertThat(getTaskUseCase.getByTaskId(taskId).getTaskId()).isEqualTo(taskId);
    }

    @Test
    void assigneeCanReadTask() {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        saveTask(taskId, userId, UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess()).thenReturn(new CurrentUserAccess(userId, false));

        assertThat(getTaskUseCase.getByTaskId(taskId).getTaskId()).isEqualTo(taskId);
    }

    @Test
    void unrelatedUserReceivesNotFound() {
        UUID taskId = UUID.randomUUID();
        saveTask(taskId, UUID.randomUUID(), UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(UUID.randomUUID(), false));

        assertThatThrownBy(() -> getTaskUseCase.getByTaskId(taskId))
            .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void missingTaskThrowsNotFoundException() {
        UUID missingTaskId = UUID.randomUUID();

        assertThatThrownBy(() -> getTaskUseCase.getByTaskId(missingTaskId))
            .isInstanceOf(TaskNotFoundException.class)
            .hasMessageContaining(missingTaskId.toString());
    }

    private void saveTask(UUID taskId, UUID assigneeUserId, UUID createdByUserId) {
        taskRepository.saveAndFlush(new TaskEntity(
            taskId,
            "Read task",
            null,
            TaskStatus.NEW,
            TaskPriority.MEDIUM,
            assigneeUserId,
            createdByUserId
        ));
    }
}
