package com.example.ai_service.controller;

import com.example.ai_service.domain.Response;
import com.example.ai_service.dto.ImproveTaskDescriptionRequestDto;
import com.example.ai_service.dto.ImproveTaskDescriptionResponseDto;
import com.example.ai_service.dto.SuggestPriorityRequestDto;
import com.example.ai_service.dto.SuggestPriorityResponseDto;
import com.example.ai_service.dto.SuggestSubtasksRequestDto;
import com.example.ai_service.dto.SuggestSubtasksResponseDto;
import com.example.ai_service.dto.SummarizeTaskRequestDto;
import com.example.ai_service.dto.SummarizeTaskResponseDto;
import com.example.ai_service.model.SuggestPriorityRequest;
import com.example.ai_service.model.SuggestPriorityResult;
import com.example.ai_service.model.SuggestSubtasksRequest;
import com.example.ai_service.model.SuggestSubtasksResult;
import com.example.ai_service.model.ImproveTaskDescriptionRequest;
import com.example.ai_service.model.ImproveTaskDescriptionResult;
import com.example.ai_service.model.SummarizeTaskRequest;
import com.example.ai_service.model.SummarizeTaskResult;
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
            @RequestBody @Valid ImproveTaskDescriptionRequestDto requestBody,
            HttpServletRequest request
    ) {
        ImproveTaskDescriptionResult result = improveTaskDescriptionUseCase.improveTaskDescription(
                new ImproveTaskDescriptionRequest(requestBody.title(), requestBody.description())
        );
        ImproveTaskDescriptionResponseDto response =
                new ImproveTaskDescriptionResponseDto(result.improvedDescription());
        return ok(request, Map.of("result", response), "Task description improved successfully.");
    }

    @PostMapping("/subtasks/suggest")
    public ResponseEntity<Response> suggestSubtasks(
            @RequestBody @Valid SuggestSubtasksRequestDto requestBody,
            HttpServletRequest request
    ) {
        SuggestSubtasksResult result = suggestSubtasksUseCase.suggestSubtasks(
                new SuggestSubtasksRequest(requestBody.title(), requestBody.description())
        );
        SuggestSubtasksResponseDto response = new SuggestSubtasksResponseDto(result.subtasks());
        return ok(request, Map.of("result", response), "Subtasks suggested successfully.");
    }

    @PostMapping("/summary")
    public ResponseEntity<Response> summarizeTask(
            @RequestBody @Valid SummarizeTaskRequestDto requestBody,
            HttpServletRequest request
    ) {
        SummarizeTaskResult result = summarizeTaskUseCase.summarizeTask(
                new SummarizeTaskRequest(requestBody.title(), requestBody.description())
        );
        SummarizeTaskResponseDto response = new SummarizeTaskResponseDto(result.summary());
        return ok(request, Map.of("result", response), "Task summarized successfully.");
    }

    @PostMapping("/priority/suggest")
    public ResponseEntity<Response> suggestPriority(
            @RequestBody @Valid SuggestPriorityRequestDto requestBody,
            HttpServletRequest request
    ) {
        SuggestPriorityResult result = suggestPriorityUseCase.suggestPriority(
                new SuggestPriorityRequest(requestBody.title(), requestBody.description())
        );
        SuggestPriorityResponseDto response = new SuggestPriorityResponseDto(result.priority(), result.reason());
        return ok(request, Map.of("result", response), "Task priority suggested successfully.");
    }

    private ResponseEntity<Response> ok(HttpServletRequest request, Map<?, ?> data, String message) {
        return ResponseEntity.ok(RequestUtils.getResponse(request, data, message, HttpStatus.OK));
    }
}