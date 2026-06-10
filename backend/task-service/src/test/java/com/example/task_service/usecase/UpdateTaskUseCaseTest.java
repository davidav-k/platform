package com.example.task_service.usecase;

import com.example.task_service.dto.TaskResponse;
import com.example.task_service.dto.UpdateTaskRequest;
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
import java.time.temporal.ChronoUnit;
import static org.assertj.core.api.Assertions.within;
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
    "spring.datasource.url=jdbc:h2:mem:update_task_use_case_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class UpdateTaskUseCaseTest {

    @Autowired
    private UpdateTaskUseCase updateTaskUseCase;

    @Autowired
    private TaskRepository taskRepository;

    @MockitoBean
    private CurrentUserAccessProvider currentUserAccessProvider;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void adminUpdatesAnyTask() {
        TaskEntity task = saveTask(UUID.randomUUID(), UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(UUID.randomUUID(), true));
        UpdateTaskRequest request = requestWithTitle("Admin update");

        TaskResponse response = updateTaskUseCase.update(task.getTaskId(), request);

        assertThat(response.getTitle()).isEqualTo("Admin update");
    }

    @Test
    void creatorUpdatesOwnTask() {
        UUID creatorUserId = UUID.randomUUID();
        TaskEntity task = saveTask(UUID.randomUUID(), creatorUserId);
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));
        UpdateTaskRequest request = requestWithTitle("Creator update");

        TaskResponse response = updateTaskUseCase.update(task.getTaskId(), request);

        assertThat(response.getTitle()).isEqualTo("Creator update");
    }

    @Test
    void assigneeUpdatesAssignedTask() {
        UUID assigneeUserId = UUID.randomUUID();
        TaskEntity task = saveTask(assigneeUserId, UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(assigneeUserId, false));
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setPriority(TaskPriority.HIGH);

        TaskResponse response = updateTaskUseCase.update(task.getTaskId(), request);

        assertThat(response.getPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void unrelatedUserReceivesNotFound() {
        TaskEntity task = saveTask(UUID.randomUUID(), UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(UUID.randomUUID(), false));

        assertThatThrownBy(() -> updateTaskUseCase.update(task.getTaskId(), requestWithTitle("Denied")))
            .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void partialUpdateChangesOnlyProvidedFields() {
        UUID assigneeUserId = UUID.randomUUID();
        UUID creatorUserId = UUID.randomUUID();
        TaskEntity task = saveTask(assigneeUserId, creatorUserId);
        String originalDescription = task.getDescription();
        TaskPriority originalPriority = task.getPriority();
        TaskStatus originalStatus = task.getStatus();
        OffsetDateTime originalCreatedAt = task.getCreatedAt();
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));

        TaskResponse response = updateTaskUseCase.update(task.getTaskId(), requestWithTitle("Only title changed"));

        assertThat(response.getTitle()).isEqualTo("Only title changed");
        assertThat(response.getDescription()).isEqualTo(originalDescription);
        assertThat(response.getPriority()).isEqualTo(originalPriority);
        assertThat(response.getStatus()).isEqualTo(originalStatus);
        assertThat(response.getAssigneeUserId()).isEqualTo(assigneeUserId);
        assertThat(response.getCreatedByUserId()).isEqualTo(creatorUserId);
        assertThat(response.getCreatedAt()).isCloseTo(originalCreatedAt, within(1, ChronoUnit.MILLIS));
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void explicitNullClearsNullableFields() {
        UUID creatorUserId = UUID.randomUUID();
        TaskEntity task = saveTask(UUID.randomUUID(), creatorUserId);
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setDescription(null);
        request.setAssigneeUserId(null);

        TaskResponse response = updateTaskUseCase.update(task.getTaskId(), request);

        assertThat(response.getDescription()).isNull();
        assertThat(response.getAssigneeUserId()).isNull();
    }

    @Test
    void rejectsEmptyUpdate() {
        assertThatThrownBy(() -> updateTaskUseCase.update(UUID.randomUUID(), new UpdateTaskRequest()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one task field must be provided");
    }

    @Test
    void rejectsNullTitleAndPriority() {
        UpdateTaskRequest nullTitle = new UpdateTaskRequest();
        nullTitle.setTitle(null);
        UpdateTaskRequest nullPriority = new UpdateTaskRequest();
        nullPriority.setPriority(null);

        assertThatThrownBy(() -> updateTaskUseCase.update(UUID.randomUUID(), nullTitle))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Title must not be blank");
        assertThatThrownBy(() -> updateTaskUseCase.update(UUID.randomUUID(), nullPriority))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Priority must not be null");
    }

    private UpdateTaskRequest requestWithTitle(String title) {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle(title);
        return request;
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
