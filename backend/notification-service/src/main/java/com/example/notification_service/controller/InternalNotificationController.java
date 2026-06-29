package com.example.notification_service.controller;

import com.example.notification_service.domain.Response;
import com.example.notification_service.dto.CreateSystemNotificationRequest;
import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.usecase.CreateSystemNotificationUseCase;
import com.example.notification_service.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/api/v1/notifications")
public class InternalNotificationController {

    private final CreateSystemNotificationUseCase createSystemNotificationUseCase;

    @PostMapping("/system")
    public ResponseEntity<Response> createSystemNotification(
        @RequestBody @Valid CreateSystemNotificationRequest request,
        HttpServletRequest httpRequest
    ) {
        NotificationResponse notification = createSystemNotificationUseCase.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RequestUtils.getResponse(
            httpRequest,
            Map.of("notification", notification),
            "System notification created successfully.",
            HttpStatus.CREATED
        ));
    }
}
