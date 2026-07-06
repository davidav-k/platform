package com.example.ai_service.controller;

import com.example.ai_service.domain.Response;
import com.example.ai_service.dto.ImproveTaskDescriptionRequest;
import com.example.ai_service.dto.ImproveTaskDescriptionResponse;
import com.example.ai_service.dto.SuggestPriorityRequest;
import com.example.ai_service.dto.SuggestPriorityResponse;
import com.example.ai_service.dto.SuggestSubtasksRequest;
import com.example.ai_service.dto.SuggestSubtasksResponse;
import com.example.ai_service.dto.SummarizeTaskRequest;
import com.example.ai_service.dto.SummarizeTaskResponse;
import com.example.ai_service.model.PrioritySuggestionRequest;
import com.example.ai_service.model.PrioritySuggestionResponse;
import com.example.ai_service.model.SubtaskSuggestionRequest;
import com.example.ai_service.model.SubtaskSuggestionResponse;
import com.example.ai_service.model.TaskDescriptionImprovementRequest;
import com.example.ai_service.model.TaskDescriptionImprovementResponse;
import com.example.ai_service.model.TaskSummaryRequest;
import com.example.ai_service.model.TaskSummaryResponse;
import com.example.ai_service.usecase.ImproveTaskDescriptionUseCase;
import com.example.ai_service.usecase.SuggestPriorityUseCase;
import com.example.ai_service.usecase.SuggestSubtasksUseCase;
import com.example.ai_service.usecase.SummarizeTaskUseCase;
import com.example.ai_service.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/tasks")
@Validated
@RequiredArgsConstructor
public class AiTaskController {

    private final ImproveTaskDescriptionUseCase improveTaskDescriptionUseCase;
    private final SuggestSubtasksUseCase suggestSubtasksUseCase;
    private final SummarizeTaskUseCase summarizeTaskUseCase;
    private final SuggestPriorityUseCase suggestPriorityUseCase;

    @PostMapping("/description/improve")
    public ResponseEntity<Response> improveTaskDescription(
            @RequestBody @Valid ImproveTaskDescriptionRequest requestBody,
            HttpServletRequest request
    ) {
        TaskDescriptionImprovementResponse result = improveTaskDescriptionUseCase.improveTaskDescription(
                new TaskDescriptionImprovementRequest(requestBody.title(), requestBody.description())
        );
        ImproveTaskDescriptionResponse response =
                new ImproveTaskDescriptionResponse(result.improvedDescription());
        return ok(request, Map.of("result", response), "Task description improved successfully.");
    }

    @PostMapping("/subtasks/suggest")
    public ResponseEntity<Response> suggestSubtasks(
            @RequestBody @Valid SuggestSubtasksRequest requestBody,
            HttpServletRequest request
    ) {
        SubtaskSuggestionResponse result = suggestSubtasksUseCase.suggestSubtasks(
                new SubtaskSuggestionRequest(requestBody.title(), requestBody.description())
        );
        SuggestSubtasksResponse response = new SuggestSubtasksResponse(result.subtasks());
        return ok(request, Map.of("result", response), "Subtasks suggested successfully.");
    }

    @PostMapping("/summary")
    public ResponseEntity<Response> summarizeTask(
            @RequestBody @Valid SummarizeTaskRequest requestBody,
            HttpServletRequest request
    ) {
        TaskSummaryResponse result = summarizeTaskUseCase.summarizeTask(
                new TaskSummaryRequest(requestBody.title(), requestBody.description())
        );
        SummarizeTaskResponse response = new SummarizeTaskResponse(result.summary());
        return ok(request, Map.of("result", response), "Task summarized successfully.");
    }

    @PostMapping("/priority/suggest")
    public ResponseEntity<Response> suggestPriority(
            @RequestBody @Valid SuggestPriorityRequest requestBody,
            HttpServletRequest request
    ) {
        PrioritySuggestionResponse result = suggestPriorityUseCase.suggestPriority(
                new PrioritySuggestionRequest(requestBody.title(), requestBody.description())
        );
        SuggestPriorityResponse response = new SuggestPriorityResponse(result.priority(), result.reason());
        return ok(request, Map.of("result", response), "Task priority suggested successfully.");
    }

    private ResponseEntity<Response> ok(HttpServletRequest request, Map<?, ?> data, String message) {
        return ResponseEntity.ok(RequestUtils.getResponse(request, data, message, HttpStatus.OK));
    }
}