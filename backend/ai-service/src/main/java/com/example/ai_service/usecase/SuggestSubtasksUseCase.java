package com.example.ai_service.usecase;

import com.example.ai_service.model.SubtaskSuggestionRequest;
import com.example.ai_service.model.SubtaskSuggestionResponse;

public interface SuggestSubtasksUseCase {

    SubtaskSuggestionResponse suggestSubtasks(SubtaskSuggestionRequest request);
}
