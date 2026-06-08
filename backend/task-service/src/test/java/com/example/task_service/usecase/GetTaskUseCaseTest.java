package com.example.task_service.usecase;

import com.example.task_service.dto.TaskResponse;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.exception.TaskNotFoundException;
import com.example.task_service.repository.TaskRepository;
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
class GetTaskUseCaseTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
        .withDatabaseName("task_service_get_task_test")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private GetTaskUseCase getTaskUseCase;

    @Autowired
    private TaskRepository taskRepository;

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
    void missingTaskThrowsNotFoundException() {
        UUID missingTaskId = UUID.randomUUID();

        assertThatThrownBy(() -> getTaskUseCase.getByTaskId(missingTaskId))
            .isInstanceOf(TaskNotFoundException.class)
            .hasMessageContaining(missingTaskId.toString());
    }
}
