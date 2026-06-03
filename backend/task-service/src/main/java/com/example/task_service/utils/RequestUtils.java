package com.example.task_service.utils;

import com.example.task_service.domain.Response;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

public final class RequestUtils {

    private RequestUtils() {
    }

    public static Response getResponse(HttpServletRequest request, Map<?, ?> data, String message, HttpStatus status) {
        return new Response(
            LocalDateTime.now().toString(),
            status.value(),
            request.getRequestURI(),
            HttpStatus.valueOf(status.value()),
            message,
            "",
            data
        );
    }
}
