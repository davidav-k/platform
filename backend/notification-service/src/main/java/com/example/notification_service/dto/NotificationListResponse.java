package com.example.notification_service.dto;

import java.util.List;

public record NotificationListResponse(List<NotificationResponse> items, PageResponse page) {

}
