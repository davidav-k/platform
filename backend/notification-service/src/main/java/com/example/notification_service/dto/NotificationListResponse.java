package com.example.notification_service.dto;

import java.util.List;

public class NotificationListResponse {

    private final List<NotificationResponse> items;
    private final PageResponse page;

    public NotificationListResponse(List<NotificationResponse> items, PageResponse page) {
        this.items = items;
        this.page = page;
    }

    public List<NotificationResponse> getItems() {
        return items;
    }

    public PageResponse getPage() {
        return page;
    }
}
