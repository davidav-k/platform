package com.example.task_service.notification;

import com.example.task_service.notification.dto.CreateNotificationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;
import java.util.Optional;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestNotificationClientTest {

    @Test
    void postsNotificationToInternalEndpoint() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://notification-service:8087");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CurrentRequestAccessTokenProvider accessTokenProvider = mock(CurrentRequestAccessTokenProvider.class);
        when(accessTokenProvider.currentAccessToken()).thenReturn(Optional.of("test-access-token"));
        RestNotificationClient client = new RestNotificationClient(builder.build(), accessTokenProvider);
        UUID recipientUserId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        server.expect(requestTo("http://notification-service:8087/internal/api/v1/notifications/system"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-access-token"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                {
                  "recipientUserId": "%s",
                  "type": "TASK_ASSIGNED",
                  "title": "New task assigned",
                  "message": "Task \\\"Task title\\\" was assigned to you",
                  "sourceService": "task-service",
                  "sourceEntityType": "TASK",
                  "sourceEntityId": "%s"
                }
                """.formatted(recipientUserId, taskId)))
            .andRespond(withSuccess());

        client.createNotification(new CreateNotificationRequest(
            recipientUserId,
            "TASK_ASSIGNED",
            "New task assigned",
            "Task \"Task title\" was assigned to you",
            "task-service",
            "TASK",
            taskId
        ));

        server.verify();
    }
}
