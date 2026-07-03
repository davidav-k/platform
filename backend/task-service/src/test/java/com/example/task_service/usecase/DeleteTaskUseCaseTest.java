package com.example.task_service.usecase;

import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.exception.TaskNotFoundException;
import com.example.task_service.outbox.OutboxEventService;
import com.example.task_service.repository.TaskRepository;
import com.example.task_service.security.CurrentUserAccessProvider;
import com.example.task_service.security.CurrentUserAccessProvider.CurrentUserAccess;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.within;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:",
    "eureka.client.enabled=false",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:delete_task_use_case_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class DeleteTaskUseCaseTest {

    @Autowired
    private DeleteTaskUseCase deleteTaskUseCase;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CurrentUserAccessProvider currentUserAccessProvider;

    @MockitoBean
    private OutboxEventService outboxEventService;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void adminSoftDeletesAnyTaskAndWritesTaskDeletedOutboxEvent() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        TaskEntity task = saveTask(UUID.randomUUID(), UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(adminUserId, true));

        deleteTaskUseCase.delete(task.getTaskId());

        TaskEntity deleted = taskRepository.findByTaskId(task.getTaskId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.getDeletedByUserId()).isEqualTo(adminUserId);
        assertThat(taskRepository.findByTaskIdAndDeletedAtIsNull(task.getTaskId())).isEmpty();

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxEventService).saveNewEvent(
            eq("TASK"),
            eq(task.getTaskId()),
            eq("TASK_DELETED"),
            payloadCaptor.capture()
        );

        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(payload.get("taskId").asText()).isEqualTo(task.getTaskId().toString());
        assertThat(payload.get("title").asText()).isEqualTo("Task to delete");
        assertThat(payload.get("status").asText()).isEqualTo("NEW");
        assertThat(payload.get("priority").asText()).isEqualTo("MEDIUM");
        assertThat(payload.get("assigneeUserId").asText()).isEqualTo(task.getAssigneeUserId().toString());
        assertThat(payload.get("createdByUserId").asText()).isEqualTo(task.getCreatedByUserId().toString());
        assertThat(payload.get("deletedByUserId").asText()).isEqualTo(adminUserId.toString());
        assertThat(OffsetDateTime.parse(payload.get("deletedAt").asText()))
                .isCloseTo(deleted.getDeletedAt(), within(1, ChronoUnit.SECONDS));
    }

    @Test
    void creatorSoftDeletesOwnTask() {
        UUID creatorUserId = UUID.randomUUID();
        TaskEntity task = saveTask(UUID.randomUUID(), creatorUserId);
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));

        deleteTaskUseCase.delete(task.getTaskId());

        TaskEntity deleted = taskRepository.findByTaskId(task.getTaskId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.getDeletedByUserId()).isEqualTo(creatorUserId);
    }

    @Test
    void assigneeCannotDeleteTaskCreatedByAnotherUser() {
        UUID assigneeUserId = UUID.randomUUID();
        TaskEntity task = saveTask(assigneeUserId, UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(assigneeUserId, false));

        assertThatThrownBy(() -> deleteTaskUseCase.delete(task.getTaskId()))
            .isInstanceOf(TaskNotFoundException.class);

        TaskEntity unchanged = taskRepository.findByTaskId(task.getTaskId()).orElseThrow();
        assertThat(unchanged.getDeletedAt()).isNull();
        assertThat(unchanged.getDeletedByUserId()).isNull();
    }

    @Test
    void unrelatedUserCannotDeleteTask() {
        TaskEntity task = saveTask(UUID.randomUUID(), UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(UUID.randomUUID(), false));

        assertThatThrownBy(() -> deleteTaskUseCase.delete(task.getTaskId()))
            .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void alreadyDeletedTaskReturnsNotFound() {
        UUID creatorUserId = UUID.randomUUID();
        TaskEntity task = saveTask(UUID.randomUUID(), creatorUserId);
        task.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        task.setDeletedByUserId(creatorUserId);
        taskRepository.saveAndFlush(task);
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));

        assertThatThrownBy(() -> deleteTaskUseCase.delete(task.getTaskId()))
            .isInstanceOf(TaskNotFoundException.class);
    }

    private TaskEntity saveTask(UUID assigneeUserId, UUID createdByUserId) {
        return taskRepository.saveAndFlush(new TaskEntity(
            UUID.randomUUID(),
            "Task to delete",
            null,
            TaskStatus.NEW,
            TaskPriority.MEDIUM,
            assigneeUserId,
            createdByUserId
        ));
    }
}
