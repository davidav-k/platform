package com.example.task_service.notification;

import com.example.task_service.notification.dto.CreateNotificationRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestNotificationClient implements NotificationClient {

    private static final String CREATE_NOTIFICATION_PATH = "/api/v1/notifications";

    private final RestClient restClient;

    public RestNotificationClient(
        @Qualifier("notificationServiceRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    @Override
    public void createNotification(CreateNotificationRequest request) {
        try {
            restClient.post()
                .uri(CREATE_NOTIFICATION_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new NotificationClientException("Notification service request failed", exception);
        }
    }
}
