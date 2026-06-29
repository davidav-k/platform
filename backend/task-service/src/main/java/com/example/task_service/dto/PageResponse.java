package com.example.task_service.dto;

public record PageResponse(
        int number,
        int size,
        long totalElements,
        int totalPages) {
}
