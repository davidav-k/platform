package com.example.ai_service.usecase;

import com.example.ai_service.model.PrioritySuggestionRequest;
import com.example.ai_service.model.PrioritySuggestionResponse;

public interface SuggestPriorityUseCase {

    PrioritySuggestionResponse suggestPriority(PrioritySuggestionRequest request);
}
