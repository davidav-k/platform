package com.example.ai_service.dto;

import com.example.ai_service.enumeration.AiTaskPriority;

public record SuggestPriorityResponse(
        AiTaskPriority priority,
        String reason
) {
}