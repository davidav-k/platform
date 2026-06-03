package com.example.task_service.usecase;

import com.example.task_service.dto.CreateTaskRequest;
import com.example.task_service.dto.CreateTaskResponse;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.repository.TaskRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:",
    "eureka.client.enabled=false",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers
class CreateTaskUseCaseTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
        .withDatabaseName("task_service_create_task_test")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CreateTaskUseCase createTaskUseCase;

    @Autowired
    private TaskRepository taskRepository;

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

        assertThat(response.getTaskId()).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Prepare create task use case");
        assertThat(response.getDescription()).isEqualTo("Implement MVP task creation.");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(response.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.getAssigneeUserId()).isEqualTo(assigneeUserId);
        assertThat(response.getCreatedByUserId()).isEqualTo(createdByUserId);
        assertThat(response.getCreatedAt()).isNotNull();

        TaskEntity persisted = taskRepository.findAll().stream()
            .filter(task -> response.getTaskId().equals(task.getTaskId()))
            .findFirst()
            .orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(persisted.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(persisted.getCreatedAt()).isNotNull();
    }

    @Test
    void defaultsPriorityToMediumWhenPriorityIsNull() {
        CreateTaskRequest request = new CreateTaskRequest("Default priority", null, null, null);

        CreateTaskResponse response = createTaskUseCase.create(request, UUID.randomUUID());

        assertThat(response.getPriority()).isEqualTo(TaskPriority.MEDIUM);
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
