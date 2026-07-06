package com.example.ai_service.model;

import com.example.ai_service.enumeration.AiTaskPriority;

public record PrioritySuggestionResponse(
        AiTaskPriority priority,
        String reason
) {
}
