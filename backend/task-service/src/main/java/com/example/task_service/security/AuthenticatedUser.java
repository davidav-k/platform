package com.example.task_service.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String username) {
}
