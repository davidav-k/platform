package com.example.task_service.controller;

import com.example.task_service.domain.Response;
import com.example.task_service.dto.CreateTaskRequest;
import com.example.task_service.dto.CreateTaskResponse;
import com.example.task_service.dto.TaskListQuery;
import com.example.task_service.dto.TaskListResponse;
import com.example.task_service.dto.TaskResponse;
import com.example.task_service.dto.UpdateTaskRequest;
import com.example.task_service.dto.UpdateTaskStatusRequest;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.security.AuthenticatedUser;
import com.example.task_service.usecase.ChangeTaskStatusUseCase;
import com.example.task_service.usecase.CreateTaskUseCase;
import com.example.task_service.usecase.GetTaskUseCase;
import com.example.task_service.usecase.ListTasksUseCase;
import com.example.task_service.usecase.UpdateTaskUseCase;
import com.example.task_service.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@Validated
public class TaskController {

    private final CreateTaskUseCase createTaskUseCase;
    private final GetTaskUseCase getTaskUseCase;
    private final ListTasksUseCase listTasksUseCase;
    private final UpdateTaskUseCase updateTaskUseCase;
    private final ChangeTaskStatusUseCase changeTaskStatusUseCase;

    public TaskController(CreateTaskUseCase createTaskUseCase, GetTaskUseCase getTaskUseCase,
                          ListTasksUseCase listTasksUseCase, UpdateTaskUseCase updateTaskUseCase,
                          ChangeTaskStatusUseCase changeTaskStatusUseCase) {
        this.createTaskUseCase = createTaskUseCase;
        this.getTaskUseCase = getTaskUseCase;
        this.listTasksUseCase = listTasksUseCase;
        this.updateTaskUseCase = updateTaskUseCase;
        this.changeTaskStatusUseCase = changeTaskStatusUseCase;
    }

@PostMapping
public ResponseEntity<Response> createTask(@RequestBody @Valid CreateTaskRequest createTaskRequest,
                                           @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                           HttpServletRequest request) {
    CreateTaskResponse task = createTaskUseCase.create(createTaskRequest, authenticatedUser.userId());
    Response response = RequestUtils.getResponse(
        request,
        Map.of("task", task),
        "Task created successfully.",
        HttpStatus.CREATED
    );
    return ResponseEntity.created(URI.create(task.getTaskId().toString())).body(response);
}

    @GetMapping("/{taskId}")
    public ResponseEntity<Response> getTask(@PathVariable UUID taskId, HttpServletRequest request) {
        TaskResponse task = getTaskUseCase.getByTaskId(taskId);
        return ResponseEntity.ok(RequestUtils.getResponse(
            request,
            Map.of("task", task),
            "Task retrieved successfully.",
            HttpStatus.OK
        ));
    }

    @PatchMapping("/{taskId}")
    public ResponseEntity<Response> updateTask(@PathVariable UUID taskId,
                                               @RequestBody @Valid UpdateTaskRequest updateTaskRequest,
                                               HttpServletRequest request) {
        TaskResponse task = updateTaskUseCase.update(taskId, updateTaskRequest);
        return ResponseEntity.ok(RequestUtils.getResponse(
            request,
            Map.of("task", task),
            "Task updated successfully.",
            HttpStatus.OK
        ));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<Response> changeTaskStatus(@PathVariable UUID taskId,
                                                     @RequestBody @Valid UpdateTaskStatusRequest requestBody,
                                                     HttpServletRequest request) {
        TaskResponse task = changeTaskStatusUseCase.changeStatus(taskId, requestBody);
        return ResponseEntity.ok(RequestUtils.getResponse(
            request,
            Map.of("task", task),
            "Task status changed successfully.",
            HttpStatus.OK
        ));
    }

    @GetMapping
    public ResponseEntity<Response> listTasks(@RequestParam(required = false) TaskStatus status,
                                              @RequestParam(required = false) TaskPriority priority,
                                              @RequestParam(required = false) UUID assigneeUserId,
                                              @RequestParam(required = false) UUID createdByUserId,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                              @RequestParam(defaultValue = "createdAt,desc") String sort,
                                              HttpServletRequest request) {
        TaskListQuery query = new TaskListQuery(
            status,
            priority,
            assigneeUserId,
            createdByUserId,
            page,
            size,
            sort
        );
        TaskListResponse tasks = listTasksUseCase.list(query);
        return ResponseEntity.ok(RequestUtils.getResponse(
            request,
            Map.of("items", tasks.getItems(), "page", tasks.getPage()),
            "Tasks retrieved successfully.",
            HttpStatus.OK
        ));
    }
}
