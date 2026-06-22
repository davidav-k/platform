package com.example.task_service.usecase;

import com.example.task_service.dto.CreateTaskRequest;
import com.example.task_service.dto.CreateTaskResponse;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.notification.TaskNotificationContext;
import com.example.task_service.notification.TaskNotificationPublisher;
import com.example.task_service.repository.TaskRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:",
    "eureka.client.enabled=false",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:create_task_use_case_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class CreateTaskUseCaseTest {

    @Autowired
    private CreateTaskUseCase createTaskUseCase;

    @Autowired
    private TaskRepository taskRepository;

    @MockitoBean
    private TaskNotificationPublisher taskNotificationPublisher;

    @Test
    void createsTaskWithNewStatusGeneratedTaskIdAndPersistsEntity() {
        UUID createdByUserId = UUID.randomUUID();
        UUID assigneeUserId = UUID.randomUUID();
        CreateTaskRequest request = new CreateTaskRequest(
            " Prepare create task use case ",
            "Implement MVP task creation.",
            TaskPriority.HIGH,
            assigneeUserId
        );

        CreateTaskResponse response = createTaskUseCase.create(request, createdByUserId);

        assertThat(response.taskId()).isNotNull();
        assertThat(response.title()).isEqualTo("Prepare create task use case");
        assertThat(response.description()).isEqualTo("Implement MVP task creation.");
        assertThat(response.status()).isEqualTo(TaskStatus.NEW);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.assigneeUserId()).isEqualTo(assigneeUserId);
        assertThat(response.createdByUserId()).isEqualTo(createdByUserId);
        assertThat(response.createdAt()).isNotNull();

        TaskEntity persisted = taskRepository.findAll().stream()
            .filter(task -> response.taskId().equals(task.getTaskId()))
            .findFirst()
            .orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(persisted.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(persisted.getCreatedAt()).isNotNull();
        verify(taskNotificationPublisher).notifyTaskAssigned(new TaskNotificationContext(
            response.taskId(), response.title(), assigneeUserId, createdByUserId
        ));
    }

    @Test
    void doesNotPublishNotificationWhenTaskHasNoAssignee() {
        CreateTaskRequest request = new CreateTaskRequest("Unassigned task", null, TaskPriority.LOW, null);

        createTaskUseCase.create(request, UUID.randomUUID());

        verify(taskNotificationPublisher, never()).notifyTaskAssigned(any());
    }

    @Test
    void persistsTaskWhenNotificationPublisherFails() {
        UUID assigneeUserId = UUID.randomUUID();
        doThrow(new IllegalStateException("notification unavailable"))
            .when(taskNotificationPublisher).notifyTaskAssigned(any());

        CreateTaskResponse response = createTaskUseCase.create(
            new CreateTaskRequest("Persist despite notification failure", null, TaskPriority.HIGH, assigneeUserId),
            UUID.randomUUID()
        );

        assertThat(response.taskId()).isNotNull();
        assertThat(taskRepository.findByTaskId(response.taskId())).isPresent();
    }

    @Test
    void defaultsPriorityToMediumWhenPriorityIsNull() {
        CreateTaskRequest request = new CreateTaskRequest("Default priority", null, null, null);

        CreateTaskResponse response = createTaskUseCase.create(request, UUID.randomUUID());

        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @Test
    void rejectsNullTitle() {
        CreateTaskRequest request = new CreateTaskRequest(null, null, TaskPriority.LOW, null);

        assertThatThrownBy(() -> createTaskUseCase.create(request, UUID.randomUUID()))
            .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void rejectsBlankTitle() {
        CreateTaskRequest request = new CreateTaskRequest("   ", null, TaskPriority.LOW, null);

        assertThatThrownBy(() -> createTaskUseCase.create(request, UUID.randomUUID()))
            .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void rejectsInvalidLengths() {
        CreateTaskRequest request = new CreateTaskRequest(
            "a".repeat(201),
            "b".repeat(5001),
            TaskPriority.LOW,
            null
        );

        assertThatThrownBy(() -> createTaskUseCase.create(request, UUID.randomUUID()))
            .isInstanceOf(ConstraintViolationException.class)
            .satisfies(error -> assertThat(((ConstraintViolationException) error).getConstraintViolations()).hasSize(2));
    }
}
