package com.example.task_service.repository;

import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
    "spring.autoconfigure.exclude=",
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:",
    "eureka.client.enabled=false",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
        .withDatabaseName("task_service_test")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsTaskWithUuidEnumsAndTimestamps() {
        UUID taskId = UUID.randomUUID();
        UUID assigneeUserId = UUID.randomUUID();
        UUID createdByUserId = UUID.randomUUID();

        TaskEntity saved = taskRepository.saveAndFlush(new TaskEntity(
            taskId,
            "Prepare MVP task persistence",
            "Create the initial persistence mapping.",
            TaskStatus.NEW,
            TaskPriority.HIGH,
            assigneeUserId,
            createdByUserId
        ));

        entityManager.clear();

        TaskEntity found = taskRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTaskId()).isEqualTo(taskId);
        assertThat(found.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(found.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(found.getAssigneeUserId()).isEqualTo(assigneeUserId);
        assertThat(found.getCreatedByUserId()).isEqualTo(createdByUserId);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getVersion()).isNotNull();
    }

    @Test
    void rejectsDuplicateTaskId() {
        UUID taskId = UUID.randomUUID();
        UUID createdByUserId = UUID.randomUUID();

        taskRepository.saveAndFlush(new TaskEntity(
            taskId,
            "First task",
            null,
            TaskStatus.NEW,
            TaskPriority.MEDIUM,
            null,
            createdByUserId
        ));

        TaskEntity duplicate = new TaskEntity(
            taskId,
            "Duplicate task",
            null,
            TaskStatus.IN_PROGRESS,
            TaskPriority.LOW,
            null,
            createdByUserId
        );

        assertThatThrownBy(() -> taskRepository.saveAndFlush(duplicate))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
