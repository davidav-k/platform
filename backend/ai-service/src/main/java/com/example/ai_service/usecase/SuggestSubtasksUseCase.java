package com.example.ai_service.usecase;

import com.example.ai_service.model.SuggestSubtasksRequest;
import com.example.ai_service.model.SuggestSubtasksResult;

public interface SuggestSubtasksUseCase {

    SuggestSubtasksResult suggestSubtasks(SuggestSubtasksRequest request);
}
