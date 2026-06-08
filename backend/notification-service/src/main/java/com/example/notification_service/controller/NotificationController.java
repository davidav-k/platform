package com.example.notification_service.controller;

import com.example.notification_service.domain.Response;
import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.usecase.CreateNotificationUseCase;
import com.example.notification_service.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final CreateNotificationUseCase createNotificationUseCase;

    public NotificationController(CreateNotificationUseCase createNotificationUseCase) {
        this.createNotificationUseCase = createNotificationUseCase;
    }

    @PostMapping
    public ResponseEntity<Response> createNotification(
            @RequestBody @Valid CreateNotificationRequest request,
            HttpServletRequest httpRequest
    ) {
        NotificationResponse notification = createNotificationUseCase.create(request);
        Response response = RequestUtils.getResponse(
                httpRequest,
                Map.of("notification", notification),
                "Notification created successfully.",
                HttpStatus.CREATED
        );
        return ResponseEntity.created(URI.create(notification.getNotificationId().toString())).body(response);
    }
}
