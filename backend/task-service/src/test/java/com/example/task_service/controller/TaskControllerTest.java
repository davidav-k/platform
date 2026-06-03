package com.example.task_service.controller;

import com.example.task_service.dto.CreateTaskRequest;
import com.example.task_service.dto.CreateTaskResponse;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.usecase.CreateTaskUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateTaskUseCase createTaskUseCase;

    @Test
    void createsTask() throws Exception {
        UUID creatorUserId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID assigneeUserId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-03T04:00:00Z");

        when(createTaskUseCase.create(any(CreateTaskRequest.class), eq(creatorUserId)))
            .thenReturn(new CreateTaskResponse(
                taskId,
                "Implement login",
                "Create login functionality",
                TaskStatus.NEW,
                TaskPriority.HIGH,
                assigneeUserId,
                creatorUserId,
                createdAt
            ));

        CreateTaskRequest request = new CreateTaskRequest(
            "Implement login",
            "Create login functionality",
            TaskPriority.HIGH,
            assigneeUserId
        );

        mockMvc.perform(post("/api/v1/tasks")
                .header("X-Created-By-User-Id", creatorUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/tasks/" + taskId))
            .andExpect(jsonPath("$.code").value(201))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.message").value("Task created successfully."))
            .andExpect(jsonPath("$.data.task.taskId").value(taskId.toString()))
            .andExpect(jsonPath("$.data.task.title").value("Implement login"))
            .andExpect(jsonPath("$.data.task.description").value("Create login functionality"))
            .andExpect(jsonPath("$.data.task.status").value("NEW"))
            .andExpect(jsonPath("$.data.task.priority").value("HIGH"))
            .andExpect(jsonPath("$.data.task.assigneeUserId").value(assigneeUserId.toString()))
            .andExpect(jsonPath("$.data.task.createdAt").value("2026-06-03T04:00:00Z"));

        verify(createTaskUseCase).create(any(CreateTaskRequest.class), eq(creatorUserId));
    }

    @Test
    void rejectsMissingTitle() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
            null,
            "Create login functionality",
            TaskPriority.HIGH,
            null
        );

        mockMvc.perform(post("/api/v1/tasks")
                .header("X-Created-By-User-Id", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("Provided arguments are not valid"))
            .andExpect(jsonPath("$.data.title").value("Title must not be blank"));
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
            "   ",
            "Create login functionality",
            TaskPriority.HIGH,
            null
        );

        mockMvc.perform(post("/api/v1/tasks")
                .header("X-Created-By-User-Id", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("Provided arguments are not valid"))
            .andExpect(jsonPath("$.data.title").value("Title must not be blank"));
    }

    @Test
    void rejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                .header("X-Created-By-User-Id", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid-json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("Request payload is not valid"));
    }
}
