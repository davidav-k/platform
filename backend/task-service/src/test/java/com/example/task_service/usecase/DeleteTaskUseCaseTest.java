package com.example.task_service.usecase;

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
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @MockitoBean
    private CurrentUserAccessProvider currentUserAccessProvider;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void adminSoftDeletesAnyTask() {
        UUID adminUserId = UUID.randomUUID();
        TaskEntity task = saveTask(UUID.randomUUID(), UUID.randomUUID());
        when(currentUserAccessProvider.currentUserAccess())
            .thenReturn(new CurrentUserAccess(adminUserId, true));

        deleteTaskUseCase.delete(task.getTaskId());

        TaskEntity deleted = taskRepository.findByTaskId(task.getTaskId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.getDeletedByUserId()).isEqualTo(adminUserId);
        assertThat(taskRepository.findByTaskIdAndDeletedAtIsNull(task.getTaskId())).isEmpty();
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
