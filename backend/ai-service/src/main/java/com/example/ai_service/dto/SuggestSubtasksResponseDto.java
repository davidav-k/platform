package com.example.ai_service.dto;

import java.util.List;

public record SuggestSubtasksResponseDto(
        List<String> subtasks
) {
}