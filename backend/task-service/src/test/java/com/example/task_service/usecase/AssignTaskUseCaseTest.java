package com.example.task_service.usecase;

import com.example.task_service.dto.AssignTaskRequest;
import com.example.task_service.dto.TaskResponse;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void adminAssignsAnyTaskAndWritesTaskAssignedOutboxEvent() throws Exception {
        UUID previousAssigneeUserId = UUID.randomUUID();
        TaskEntity task = saveTask(previousAssigneeUserId, UUID.randomUUID());
        UUID newAssigneeUserId = UUID.randomUUID();
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(UUID.randomUUID(), true));

        TaskResponse response = assignTaskUseCase.assign(
            task.getTaskId(),
            new AssignTaskRequest(newAssigneeUserId)
        );

        assertThat(response.assigneeUserId()).isEqualTo(newAssigneeUserId);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxEventService).saveNewEvent(
            eq("TASK"),
            eq(task.getTaskId()),
            eq("TASK_ASSIGNED"),
            payloadCaptor.capture()
        );

        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(payload.get("taskId").asText()).isEqualTo(task.getTaskId().toString());
        assertThat(payload.get("title").asText()).isEqualTo("Assignment task");
        assertThat(payload.get("status").asText()).isEqualTo("NEW");
        assertThat(payload.get("priority").asText()).isEqualTo("MEDIUM");
        assertThat(payload.get("previousAssigneeUserId").asText()).isEqualTo(previousAssigneeUserId.toString());
        assertThat(payload.get("newAssigneeUserId").asText()).isEqualTo(newAssigneeUserId.toString());
        assertThat(payload.get("createdByUserId").asText()).isEqualTo(task.getCreatedByUserId().toString());
        assertThat(OffsetDateTime.parse(payload.get("updatedAt").asText())).isEqualTo(response.updatedAt());
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

        assertThat(response.assigneeUserId()).isEqualTo(newAssigneeUserId);
        verify(outboxEventService).saveNewEvent(eq("TASK"), eq(task.getTaskId()), eq("TASK_ASSIGNED"), any());
    }

    @Test
    void creatorCanUnassignTask() throws Exception {
        UUID creatorUserId = UUID.randomUUID();
        TaskEntity task = saveTask(UUID.randomUUID(), creatorUserId);
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));

        TaskResponse response = assignTaskUseCase.assign(
            task.getTaskId(),
            new AssignTaskRequest(null)
        );

        assertThat(response.assigneeUserId()).isNull();
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxEventService).saveNewEvent(eq("TASK"), eq(task.getTaskId()), eq("TASK_ASSIGNED"), payloadCaptor.capture());
        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(payload.get("previousAssigneeUserId").asText()).isEqualTo(task.getAssigneeUserId().toString());
        assertThat(payload.get("newAssigneeUserId").isNull()).isTrue();
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
        verify(outboxEventService, never()).saveNewEvent(any(), any(), any(), any());
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
        verify(outboxEventService, never()).saveNewEvent(any(), any(), any(), any());
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
        verify(outboxEventService, never()).saveNewEvent(any(), any(), any(), any());
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
        verify(outboxEventService).saveNewEvent(eq("TASK"), eq(taskId), eq("TASK_ASSIGNED"), any());
    }

    @Test
    void assigningSameAssigneeStillSavesAndWritesOutboxEvent() throws Exception {
        UUID creatorUserId = UUID.randomUUID();
        UUID assigneeUserId = UUID.randomUUID();
        TaskEntity task = saveTask(assigneeUserId, creatorUserId);
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));

        TaskResponse response = assignTaskUseCase.assign(
            task.getTaskId(),
            new AssignTaskRequest(assigneeUserId)
        );

        assertThat(response.assigneeUserId()).isEqualTo(assigneeUserId);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxEventService).saveNewEvent(
            eq("TASK"),
            eq(task.getTaskId()),
            eq("TASK_ASSIGNED"),
            payloadCaptor.capture()
        );

        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(payload.get("previousAssigneeUserId").asText()).isEqualTo(assigneeUserId.toString());
        assertThat(payload.get("newAssigneeUserId").asText()).isEqualTo(assigneeUserId.toString());
    }

    @Test
    void omittedAssigneeUserIdIsRejected() {
        assertThatThrownBy(() -> assignTaskUseCase.assign(
            UUID.randomUUID(),
            new AssignTaskRequest()
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Assignee user ID must be provided");
        verify(outboxEventService, never()).saveNewEvent(any(), any(), any(), any());
    }

    @Test
    void outboxFailureRollsBackAssignment() {
        UUID creatorUserId = UUID.randomUUID();
        UUID previousAssigneeUserId = UUID.randomUUID();
        TaskEntity task = saveTask(previousAssigneeUserId, creatorUserId);
        UUID newAssigneeUserId = UUID.randomUUID();
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(creatorUserId, false));
        doThrow(new IllegalStateException("outbox unavailable"))
            .when(outboxEventService).saveNewEvent(any(), any(), any(), any());

        assertThatThrownBy(() -> assignTaskUseCase.assign(
            task.getTaskId(),
            new AssignTaskRequest(newAssigneeUserId)
        )).isInstanceOf(IllegalStateException.class)
            .hasMessage("outbox unavailable");

        assertThat(taskRepository.findByTaskId(task.getTaskId()).orElseThrow().getAssigneeUserId())
            .isEqualTo(previousAssigneeUserId);
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
