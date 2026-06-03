package com.example.task_service.controller;

import com.example.task_service.domain.Response;
import com.example.task_service.dto.CreateTaskRequest;
import com.example.task_service.dto.CreateTaskResponse;
import com.example.task_service.usecase.CreateTaskUseCase;
import com.example.task_service.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final CreateTaskUseCase createTaskUseCase;

    public TaskController(CreateTaskUseCase createTaskUseCase) {
        this.createTaskUseCase = createTaskUseCase;
    }

    @PostMapping
    public ResponseEntity<Response> createTask(@RequestBody @Valid CreateTaskRequest createTaskRequest,
                                               @RequestHeader("X-Created-By-User-Id") UUID createdByUserId,
                                               HttpServletRequest request) {
        CreateTaskResponse task = createTaskUseCase.create(createTaskRequest, createdByUserId);
        Response response = RequestUtils.getResponse(
            request,
            Map.of("task", task),
            "Task created successfully.",
            HttpStatus.CREATED
        );
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + task.getTaskId())).body(response);
    }
}
