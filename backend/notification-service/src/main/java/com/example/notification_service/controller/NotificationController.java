package com.example.notification_service.controller;

import com.example.notification_service.domain.Response;
import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.dto.NotificationListQuery;
import com.example.notification_service.dto.NotificationListResponse;
import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;
import com.example.notification_service.usecase.CreateNotificationUseCase;
import com.example.notification_service.usecase.GetNotificationUseCase;
import com.example.notification_service.usecase.ListNotificationsUseCase;
import com.example.notification_service.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Validated
public class NotificationController {

    private final CreateNotificationUseCase createNotificationUseCase;
    private final GetNotificationUseCase getNotificationUseCase;
    private final ListNotificationsUseCase listNotificationsUseCase;

    public NotificationController(CreateNotificationUseCase createNotificationUseCase,
                                  GetNotificationUseCase getNotificationUseCase,
                                  ListNotificationsUseCase listNotificationsUseCase) {
        this.createNotificationUseCase = createNotificationUseCase;
        this.getNotificationUseCase = getNotificationUseCase;
        this.listNotificationsUseCase = listNotificationsUseCase;
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

    @GetMapping("/{notificationId}")
    public ResponseEntity<Response> getNotification(@PathVariable UUID notificationId,
                                                    HttpServletRequest request) {
        NotificationResponse notification = getNotificationUseCase.getByNotificationId(notificationId);
        return ResponseEntity.ok(RequestUtils.getResponse(
                request,
                Map.of("notification", notification),
                "Notification retrieved successfully.",
                HttpStatus.OK
        ));
    }

    @GetMapping
    public ResponseEntity<Response> listNotifications(
            @RequestParam(required = false) UUID recipientUserId,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            HttpServletRequest request
    ) {
        NotificationListResponse notifications = listNotificationsUseCase.list(new NotificationListQuery(
                recipientUserId, status, channel, type, page, size, sort
        ));
        return ResponseEntity.ok(RequestUtils.getResponse(
                request,
                Map.of("items", notifications.getItems(), "page", notifications.getPage()),
                "Notifications retrieved successfully.",
                HttpStatus.OK
        ));
    }
}
