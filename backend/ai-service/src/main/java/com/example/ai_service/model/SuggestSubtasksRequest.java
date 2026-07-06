package com.example.ai_service.model;

public record SuggestSubtasksRequest(
        String title,
        String description
) {
}
