package com.example.ai_service.provider;

import com.example.ai_service.model.PrioritySuggestionRequest;
import com.example.ai_service.model.PrioritySuggestionResponse;
import com.example.ai_service.model.SubtaskSuggestionRequest;
import com.example.ai_service.model.SubtaskSuggestionResponse;
import com.example.ai_service.model.TaskDescriptionImprovementRequest;
import com.example.ai_service.model.TaskDescriptionImprovementResponse;
import com.example.ai_service.model.TaskSummaryRequest;
import com.example.ai_service.model.TaskSummaryResponse;

public interface AiProvider {

    TaskDescriptionImprovementResponse improveTaskDescription(TaskDescriptionImprovementRequest request);

    SubtaskSuggestionResponse suggestSubtasks(SubtaskSuggestionRequest request);

    TaskSummaryResponse summarizeTask(TaskSummaryRequest request);

    PrioritySuggestionResponse suggestPriority(PrioritySuggestionRequest request);
}
