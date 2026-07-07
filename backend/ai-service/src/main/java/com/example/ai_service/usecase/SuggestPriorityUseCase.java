package com.example.ai_service.usecase;

import com.example.ai_service.model.SuggestPriorityRequest;
import com.example.ai_service.model.SuggestPriorityResult;

public interface SuggestPriorityUseCase {

    SuggestPriorityResult suggestPriority(SuggestPriorityRequest request);
}
