package com.example.ai_service.model;

import java.util.List;

public record SuggestSubtasksResult(
        List<String> subtasks
) {
}
