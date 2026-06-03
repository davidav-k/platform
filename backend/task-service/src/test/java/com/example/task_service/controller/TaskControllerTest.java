package com.example.task_service.controller;

import com.example.task_service.dto.CreateTaskRequest;
import com.example.task_service.dto.CreateTaskResponse;
import com.example.task_service.dto.PageResponse;
import com.example.task_service.dto.TaskListQuery;
import com.example.task_service.dto.TaskListResponse;
import com.example.task_service.dto.TaskResponse;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.exception.TaskNotFoundException;
import com.example.task_service.usecase.CreateTaskUseCase;
import com.example.task_service.usecase.GetTaskUseCase;
import com.example.task_service.usecase.ListTasksUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @MockitoBean
    private GetTaskUseCase getTaskUseCase;

    @MockitoBean
    private ListTasksUseCase listTasksUseCase;

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

    @Test
    void getsTaskByTaskId() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID creatorUserId = UUID.randomUUID();
        UUID assigneeUserId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-03T04:00:00Z");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-03T05:00:00Z");

        when(getTaskUseCase.getByTaskId(taskId))
            .thenReturn(new TaskResponse(
                taskId,
                "Implement login",
                "Create login functionality",
                TaskStatus.NEW,
                TaskPriority.HIGH,
                assigneeUserId,
                creatorUserId,
                createdAt,
                updatedAt
            ));

        mockMvc.perform(get("/api/v1/tasks/{taskId}", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.message").value("Task retrieved successfully."))
            .andExpect(jsonPath("$.data.task.taskId").value(taskId.toString()))
            .andExpect(jsonPath("$.data.task.title").value("Implement login"))
            .andExpect(jsonPath("$.data.task.description").value("Create login functionality"))
            .andExpect(jsonPath("$.data.task.status").value("NEW"))
            .andExpect(jsonPath("$.data.task.priority").value("HIGH"))
            .andExpect(jsonPath("$.data.task.assigneeUserId").value(assigneeUserId.toString()))
            .andExpect(jsonPath("$.data.task.createdByUserId").value(creatorUserId.toString()))
            .andExpect(jsonPath("$.data.task.createdAt").value("2026-06-03T04:00:00Z"))
            .andExpect(jsonPath("$.data.task.updatedAt").value("2026-06-03T05:00:00Z"))
            .andExpect(jsonPath("$.data.task.id").doesNotExist());

        verify(getTaskUseCase).getByTaskId(taskId);
    }

    @Test
    void missingTaskReturnsNotFound() throws Exception {
        UUID taskId = UUID.randomUUID();

        when(getTaskUseCase.getByTaskId(taskId))
            .thenThrow(new TaskNotFoundException(taskId));

        mockMvc.perform(get("/api/v1/tasks/{taskId}", taskId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.status").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Task not found."));
    }

    @Test
    void invalidTaskIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/{taskId}", "not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("Provided arguments are not valid"))
            .andExpect(jsonPath("$.data.taskId").value("Value has an invalid format"));
    }

    @Test
    void listsTasks() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID creatorUserId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-03T04:00:00Z");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-03T05:00:00Z");

        when(listTasksUseCase.list(any(TaskListQuery.class)))
            .thenReturn(new TaskListResponse(
                List.of(new TaskResponse(
                    taskId,
                    "Implement login",
                    "Create login functionality",
                    TaskStatus.NEW,
                    TaskPriority.HIGH,
                    null,
                    creatorUserId,
                    createdAt,
                    updatedAt
                )),
                new PageResponse(0, 20, 1, 1)
            ));

        mockMvc.perform(get("/api/v1/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.message").value("Tasks retrieved successfully."))
            .andExpect(jsonPath("$.data.items[0].taskId").value(taskId.toString()))
            .andExpect(jsonPath("$.data.items[0].title").value("Implement login"))
            .andExpect(jsonPath("$.data.items[0].id").doesNotExist())
            .andExpect(jsonPath("$.data.page.number").value(0))
            .andExpect(jsonPath("$.data.page.size").value(20))
            .andExpect(jsonPath("$.data.page.totalElements").value(1))
            .andExpect(jsonPath("$.data.page.totalPages").value(1));

        verify(listTasksUseCase).list(argThat(query ->
            query.getStatus() == null
                && query.getPriority() == null
                && query.getAssigneeUserId() == null
                && query.getCreatedByUserId() == null
                && query.getPage() == 0
                && query.getSize() == 20
                && "createdAt,desc".equals(query.getSort())
        ));
    }

    @Test
    void listsTasksWithFilters() throws Exception {
        UUID assigneeUserId = UUID.randomUUID();
        UUID createdByUserId = UUID.randomUUID();

        when(listTasksUseCase.list(any(TaskListQuery.class)))
            .thenReturn(new TaskListResponse(List.of(), new PageResponse(0, 10, 0, 0)));

        mockMvc.perform(get("/api/v1/tasks")
                .param("status", "NEW")
                .param("priority", "HIGH")
                .param("assigneeUserId", assigneeUserId.toString())
                .param("createdByUserId", createdByUserId.toString())
                .param("page", "0")
                .param("size", "10")
                .param("sort", "updatedAt,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.page.size").value(10));

        verify(listTasksUseCase).list(argThat(query ->
            query.getStatus() == TaskStatus.NEW
                && query.getPriority() == TaskPriority.HIGH
                && assigneeUserId.equals(query.getAssigneeUserId())
                && createdByUserId.equals(query.getCreatedByUserId())
                && query.getPage() == 0
                && query.getSize() == 10
                && "updatedAt,asc".equals(query.getSort())
        ));
    }

    @Test
    void invalidStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tasks").param("status", "UNKNOWN"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("Provided arguments are not valid"))
            .andExpect(jsonPath("$.data.status").value("Value has an invalid format"));
    }

    @Test
    void invalidPriorityReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tasks").param("priority", "CRITICAL"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("Provided arguments are not valid"))
            .andExpect(jsonPath("$.data.priority").value("Value has an invalid format"));
    }

    @Test
    void invalidPageReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tasks").param("page", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("Provided arguments are not valid"));
    }

    @Test
    void invalidSizeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tasks").param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("Provided arguments are not valid"));
    }
}
