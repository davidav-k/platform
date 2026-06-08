package com.example.task_service.notification;

import com.example.task_service.notification.dto.CreateNotificationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestNotificationClientTest {

    @Test
    void postsNotificationToInternalEndpoint() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://notification-service:8087");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestNotificationClient client = new RestNotificationClient(builder.build());
        UUID recipientUserId = UUID.randomUUID();

        server.expect(requestTo("http://notification-service:8087/api/v1/notifications"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                {
                  "recipientUserId": "%s",
                  "type": "TASK_ASSIGNED",
                  "channel": "IN_APP",
                  "subject": "New task assigned",
                  "body": "You have been assigned task: Task title"
                }
                """.formatted(recipientUserId)))
            .andRespond(withSuccess());

        client.createNotification(new CreateNotificationRequest(
            recipientUserId,
            "TASK_ASSIGNED",
            "IN_APP",
            "New task assigned",
            "You have been assigned task: Task title"
        ));

        server.verify();
    }
}
