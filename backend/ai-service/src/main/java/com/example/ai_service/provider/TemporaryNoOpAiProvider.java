package com.example.ai_service.provider;

import com.example.ai_service.enumeration.AiTaskPriority;
import com.example.ai_service.model.SuggestPriorityRequest;
import com.example.ai_service.model.SuggestPriorityResult;
import com.example.ai_service.model.SuggestSubtasksRequest;
import com.example.ai_service.model.SuggestSubtasksResult;
import com.example.ai_service.model.ImproveTaskDescriptionRequest;
import com.example.ai_service.model.ImproveTaskDescriptionResult;
import com.example.ai_service.model.SummarizeTaskRequest;
import com.example.ai_service.model.SummarizeTaskResult;
import java.util.List;

/**
 * Temporary dev/test fallback so the service context can start before a real AI provider is added.
 */
public class TemporaryNoOpAiProvider implements AiProvider {

    @Override
    public ImproveTaskDescriptionResult improveTaskDescription(ImproveTaskDescriptionRequest request) {
        return new ImproveTaskDescriptionResult(request.description().strip());
    }

    @Override
    public SuggestSubtasksResult suggestSubtasks(SuggestSubtasksRequest request) {
        return new SuggestSubtasksResult(List.of());
    }

    @Override
    public SummarizeTaskResult summarizeTask(SummarizeTaskRequest request) {
        return new SummarizeTaskResult(request.title().strip());
    }

    @Override
    public SuggestPriorityResult suggestPriority(SuggestPriorityRequest request) {
        return new SuggestPriorityResult(AiTaskPriority.MEDIUM, "Temporary no-op provider default");
    }
}
