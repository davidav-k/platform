package com.example.ai_service.usecase.impl;

import com.example.ai_service.model.SuggestPriorityRequest;
import com.example.ai_service.model.SuggestPriorityResult;
import com.example.ai_service.model.SuggestSubtasksRequest;
import com.example.ai_service.model.SuggestSubtasksResult;
import com.example.ai_service.model.ImproveTaskDescriptionRequest;
import com.example.ai_service.model.ImproveTaskDescriptionResult;
import com.example.ai_service.model.SummarizeTaskRequest;
import com.example.ai_service.model.SummarizeTaskResult;
import com.example.ai_service.outbox.AiOperationAuditRecorder;
import com.example.ai_service.outbox.AiOutboxEventTypes;
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
    private final AiOperationAuditRecorder auditRecorder;

    @Override
    public ImproveTaskDescriptionResult improveTaskDescription(ImproveTaskDescriptionRequest request) {
        validateTaskText(request, "Improve task description request is required");
        ImproveTaskDescriptionResult result = aiProvider.improveTaskDescription(request);
        auditRecorder.recordSuccess(AiOutboxEventTypes.TASK_DESCRIPTION_IMPROVED);
        return result;
    }

    @Override
    public SuggestSubtasksResult suggestSubtasks(SuggestSubtasksRequest request) {
        validateTaskText(request, "Suggest subtasks request is required");
        SuggestSubtasksResult result = aiProvider.suggestSubtasks(request);
        auditRecorder.recordSuccess(AiOutboxEventTypes.SUBTASKS_SUGGESTED);
        return result;
    }

    @Override
    public SummarizeTaskResult summarizeTask(SummarizeTaskRequest request) {
        validateTaskText(request, "Summarize task request is required");
        SummarizeTaskResult result = aiProvider.summarizeTask(request);
        auditRecorder.recordSuccess(AiOutboxEventTypes.TASK_SUMMARIZED);
        return result;
    }

    @Override
    public SuggestPriorityResult suggestPriority(SuggestPriorityRequest request) {
        validateTaskText(request, "Suggest priority request is required");
        SuggestPriorityResult result = aiProvider.suggestPriority(request);
        auditRecorder.recordSuccess(AiOutboxEventTypes.PRIORITY_SUGGESTED);
        return result;
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
