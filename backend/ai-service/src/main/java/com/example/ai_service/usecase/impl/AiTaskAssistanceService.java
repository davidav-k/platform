package com.example.ai_service.usecase.impl;

import com.example.ai_service.model.SuggestPriorityRequest;
import com.example.ai_service.model.SuggestPriorityResult;
import com.example.ai_service.model.SuggestSubtasksRequest;
import com.example.ai_service.model.SuggestSubtasksResult;
import com.example.ai_service.model.ImproveTaskDescriptionRequest;
import com.example.ai_service.model.ImproveTaskDescriptionResult;
import com.example.ai_service.model.SummarizeTaskRequest;
import com.example.ai_service.model.SummarizeTaskResult;
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
    public ImproveTaskDescriptionResult improveTaskDescription(ImproveTaskDescriptionRequest request) {
        validateTaskText(request, "Improve task description request is required");
        return aiProvider.improveTaskDescription(request);
    }

    @Override
    public SuggestSubtasksResult suggestSubtasks(SuggestSubtasksRequest request) {
        validateTaskText(request, "Suggest subtasks request is required");
        return aiProvider.suggestSubtasks(request);
    }

    @Override
    public SummarizeTaskResult summarizeTask(SummarizeTaskRequest request) {
        validateTaskText(request, "Summarize task request is required");
        return aiProvider.summarizeTask(request);
    }

    @Override
    public SuggestPriorityResult suggestPriority(SuggestPriorityRequest request) {
        validateTaskText(request, "Suggest priority request is required");
        return aiProvider.suggestPriority(request);
    }

    private void validateTaskText(ImproveTaskDescriptionRequest request, String nullMessage) {
        validateTaskText(request, request == null ? null : request.title(),
                request == null ? null : request.description(), nullMessage);
    }

    private void validateTaskText(SuggestSubtasksRequest request, String nullMessage) {
        validateTaskText(request, request == null ? null : request.title(),
                request == null ? null : request.description(), nullMessage);
    }

    private void validateTaskText(SummarizeTaskRequest request, String nullMessage) {
        validateTaskText(request, request == null ? null : request.title(),
                request == null ? null : request.description(), nullMessage);
    }

    private void validateTaskText(SuggestPriorityRequest request, String nullMessage) {
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
