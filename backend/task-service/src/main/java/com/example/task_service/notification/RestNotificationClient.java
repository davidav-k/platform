package com.example.task_service.notification;

import com.example.task_service.notification.dto.CreateNotificationRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestNotificationClient implements NotificationClient {

    private static final String CREATE_NOTIFICATION_PATH = "/internal/api/v1/notifications/system";

    private final RestClient restClient;
    private final CurrentRequestAccessTokenProvider accessTokenProvider;

    public RestNotificationClient(
        @Qualifier("notificationServiceRestClient") RestClient restClient,
        CurrentRequestAccessTokenProvider accessTokenProvider
    ) {
        this.restClient = restClient;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public void createNotification(CreateNotificationRequest request) {
        try {
            String accessToken = accessTokenProvider.currentAccessToken()
                .orElseThrow(() -> new NotificationClientException(
                    "No access token is available for notification service request", null
                ));
            restClient.post()
                .uri(CREATE_NOTIFICATION_PATH)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new NotificationClientException("Notification service request failed", exception);
        }
    }
}
