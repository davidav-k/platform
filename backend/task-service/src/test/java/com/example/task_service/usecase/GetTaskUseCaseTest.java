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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
