package com.example.ai_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuggestSubtasksRequestDto(
        @NotBlank(message = "Title must not be blank")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @NotBlank(message = "Description must not be blank")
        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description
) {
}