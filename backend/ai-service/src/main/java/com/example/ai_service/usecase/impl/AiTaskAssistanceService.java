package com.example.ai_service.usecase.impl;

import com.example.ai_service.model.PrioritySuggestionRequest;
import com.example.ai_service.model.PrioritySuggestionResponse;
import com.example.ai_service.model.SubtaskSuggestionRequest;
import com.example.ai_service.model.SubtaskSuggestionResponse;
import com.example.ai_service.model.TaskDescriptionImprovementRequest;
import com.example.ai_service.model.TaskDescriptionImprovementResponse;
import com.example.ai_service.model.TaskSummaryRequest;
import com.example.ai_service.model.TaskSummaryResponse;
import com.example.ai_service.provider.AiProvider;
import com.example.ai_service.usecase.ImproveTaskDescriptionUseCase;
import com.example.ai_service.usecase.SuggestPriorityUseCase;
import com.example.ai_service.usecase.SuggestSubtasksUseCase;
import com.example.ai_service.usecase.SummarizeTaskUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTaskAssistanceService implements ImproveTaskDescriptionUseCase,
        SuggestSubtasksUseCase,
        SummarizeTaskUseCase,
        SuggestPriorityUseCase {

    private final AiProvider aiProvider;

    @Override
    public TaskDescriptionImprovementResponse improveTaskDescription(TaskDescriptionImprovementRequest request) {
        validateTaskText(request, "Improve task description request is required");
        return aiProvider.improveTaskDescription(request);
    }

    @Override
    public SubtaskSuggestionResponse suggestSubtasks(SubtaskSuggestionRequest request) {
        validateTaskText(request, "Suggest subtasks request is required");
        return aiProvider.suggestSubtasks(request);
    }

    @Override
    public TaskSummaryResponse summarizeTask(TaskSummaryRequest request) {
        validateTaskText(request, "Summarize task request is required");
        return aiProvider.summarizeTask(request);
    }

    @Override
    public PrioritySuggestionResponse suggestPriority(PrioritySuggestionRequest request) {
        validateTaskText(request, "Suggest priority request is required");
        return aiProvider.suggestPriority(request);
    }

    private void validateTaskText(TaskDescriptionImprovementRequest request, String nullMessage) {
        validateTaskText(request, request == null ? null : request.title(),
                request == null ? null : request.description(), nullMessage);
    }

    private void validateTaskText(SubtaskSuggestionRequest request, String nullMessage) {
        validateTaskText(request, request == null ? null : request.title(),
                request == null ? null : request.description(), nullMessage);
    }

    private void validateTaskText(TaskSummaryRequest request, String nullMessage) {
        validateTaskText(request, request == null ? null : request.title(),
                request == null ? null : request.description(), nullMessage);
    }

    private void validateTaskText(PrioritySuggestionRequest request, String nullMessage) {
        validateTaskText(request, request == null ? null : request.title(),
                request == null ? null : request.description(), nullMessage);
    }

    private void validateTaskText(Object request, String title, String description, String nullMessage) {
        if (request == null) {
            throw new IllegalArgumentException(nullMessage);
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Task description is required");
        }
    }
}
