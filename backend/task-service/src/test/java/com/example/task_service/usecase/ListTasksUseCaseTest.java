package com.example.task_service.usecase;

import com.example.task_service.dto.TaskListQuery;
import com.example.task_service.dto.TaskListResponse;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:",
    "eureka.client.enabled=false",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers
class ListTasksUseCaseTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
        .withDatabaseName("task_service_list_tasks_test")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ListTasksUseCase listTasksUseCase;

    @Autowired
    private TaskRepository taskRepository;

    private UUID assigneeUserId;
    private UUID createdByUserId;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        assigneeUserId = UUID.randomUUID();
        createdByUserId = UUID.randomUUID();
        saveTask("New high", TaskStatus.NEW, TaskPriority.HIGH, assigneeUserId, createdByUserId);
        saveTask("Done low", TaskStatus.DONE, TaskPriority.LOW, UUID.randomUUID(), createdByUserId);
        saveTask("Progress medium", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, assigneeUserId, UUID.randomUUID());
    }

    @Test
    void listsAllTasks() {
        TaskListResponse response = listTasksUseCase.list(query(null, null, null, null, 0, 20));

        assertThat(response.getItems()).hasSize(3);
        assertThat(response.getPage().getNumber()).isZero();
        assertThat(response.getPage().getSize()).isEqualTo(20);
        assertThat(response.getPage().getTotalElements()).isEqualTo(3);
        assertThat(response.getPage().getTotalPages()).isEqualTo(1);
    }

    @Test
    void filtersByStatus() {
        TaskListResponse response = listTasksUseCase.list(query(TaskStatus.NEW, null, null, null, 0, 20));

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getStatus()).isEqualTo(TaskStatus.NEW);
    }

    @Test
    void filtersByPriority() {
        TaskListResponse response = listTasksUseCase.list(query(null, TaskPriority.HIGH, null, null, 0, 20));

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void filtersByAssigneeUserId() {
        TaskListResponse response = listTasksUseCase.list(query(null, null, assigneeUserId, null, 0, 20));

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems()).allMatch(task -> assigneeUserId.equals(task.getAssigneeUserId()));
    }

    @Test
    void filtersByCreatedByUserId() {
        TaskListResponse response = listTasksUseCase.list(query(null, null, null, createdByUserId, 0, 20));

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems()).allMatch(task -> createdByUserId.equals(task.getCreatedByUserId()));
    }

    @Test
    void returnsPaginationMetadata() {
        TaskListResponse response = listTasksUseCase.list(query(null, null, null, null, 1, 2));

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getPage().getNumber()).isEqualTo(1);
        assertThat(response.getPage().getSize()).isEqualTo(2);
        assertThat(response.getPage().getTotalElements()).isEqualTo(3);
        assertThat(response.getPage().getTotalPages()).isEqualTo(2);
    }

    @Test
    void responseDoesNotExposeDatabaseId() {
        TaskListResponse response = listTasksUseCase.list(query(null, null, null, null, 0, 20));

        assertThat(response.getItems().get(0).getTaskId()).isNotNull();
        assertThat(response.getItems().get(0).getCreatedAt()).isNotNull();
        assertThat(response.getItems().get(0).getUpdatedAt()).isNotNull();
    }

    private TaskListQuery query(TaskStatus status, TaskPriority priority, UUID assigneeUserId,
                                UUID createdByUserId, int page, int size) {
        return new TaskListQuery(status, priority, assigneeUserId, createdByUserId, page, size, "createdAt,desc");
    }

    private void saveTask(String title, TaskStatus status, TaskPriority priority, UUID assigneeUserId,
                          UUID createdByUserId) {
        taskRepository.saveAndFlush(new TaskEntity(
            UUID.randomUUID(),
            title,
            null,
            status,
            priority,
            assigneeUserId,
            createdByUserId
        ));
    }
}
