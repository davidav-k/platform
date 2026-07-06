package com.example.ai_service.provider;

import com.example.ai_service.enumeration.AiTaskPriority;
import com.example.ai_service.model.PrioritySuggestionRequest;
import com.example.ai_service.model.PrioritySuggestionResponse;
import com.example.ai_service.model.SubtaskSuggestionRequest;
import com.example.ai_service.model.SubtaskSuggestionResponse;
import com.example.ai_service.model.TaskDescriptionImprovementRequest;
import com.example.ai_service.model.TaskDescriptionImprovementResponse;
import com.example.ai_service.model.TaskSummaryRequest;
import com.example.ai_service.model.TaskSummaryResponse;
import java.util.List;

/**
 * Temporary dev/test fallback so the service context can start before a real AI provider is added.
 */
public class TemporaryNoOpAiProvider implements AiProvider {

    @Override
    public TaskDescriptionImprovementResponse improveTaskDescription(TaskDescriptionImprovementRequest request) {
        return new TaskDescriptionImprovementResponse(request.description().strip());
    }

    @Override
    public SubtaskSuggestionResponse suggestSubtasks(SubtaskSuggestionRequest request) {
        return new SubtaskSuggestionResponse(List.of());
    }

    @Override
    public TaskSummaryResponse summarizeTask(TaskSummaryRequest request) {
        return new TaskSummaryResponse(request.title().strip());
    }

    @Override
    public PrioritySuggestionResponse suggestPriority(PrioritySuggestionRequest request) {
        return new PrioritySuggestionResponse(AiTaskPriority.MEDIUM, "Temporary no-op provider default");
    }
}
