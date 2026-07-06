package com.example.ai_service.dto;

import java.util.List;

public record SuggestSubtasksResponse(
        List<String> subtasks
) {
}