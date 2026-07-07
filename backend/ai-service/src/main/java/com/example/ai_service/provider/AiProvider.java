package com.example.ai_service.provider;

import com.example.ai_service.model.SuggestPriorityRequest;
import com.example.ai_service.model.SuggestPriorityResult;
import com.example.ai_service.model.SuggestSubtasksRequest;
import com.example.ai_service.model.SuggestSubtasksResult;
import com.example.ai_service.model.ImproveTaskDescriptionRequest;
import com.example.ai_service.model.ImproveTaskDescriptionResult;
import com.example.ai_service.model.SummarizeTaskRequest;
import com.example.ai_service.model.SummarizeTaskResult;

public interface AiProvider {

    ImproveTaskDescriptionResult improveTaskDescription(ImproveTaskDescriptionRequest request);

    SuggestSubtasksResult suggestSubtasks(SuggestSubtasksRequest request);

    SummarizeTaskResult summarizeTask(SummarizeTaskRequest request);

    SuggestPriorityResult suggestPriority(SuggestPriorityRequest request);
}
