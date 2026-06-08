package com.example.notification_service.controller;

import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;
import com.example.notification_service.usecase.CreateNotificationUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = NotificationController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false"
        }
)
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateNotificationUseCase createNotificationUseCase;

    @Test
    void createsNotificationWithStandardEnvelope() throws Exception {
        UUID notificationId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-08T10:00:00Z");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-08T10:00:00Z");

        when(createNotificationUseCase.create(any(CreateNotificationRequest.class)))
                .thenReturn(new NotificationResponse(
                        notificationId,
                        recipientUserId,
                        NotificationType.TASK_ASSIGNED,
                        NotificationChannel.EMAIL,
                        "Task assigned",
                        "A task was assigned to you.",
                        NotificationStatus.PENDING,
                        createdAt,
                        updatedAt,
                        null,
                        null
                ));

        CreateNotificationRequest request = new CreateNotificationRequest(
                recipientUserId,
                NotificationType.TASK_ASSIGNED,
                NotificationChannel.EMAIL,
                "Task assigned",
                "A task was assigned to you."
        );

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", notificationId.toString()))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.path").value("/api/v1/notifications"))
                .andExpect(jsonPath("$.message").value("Notification created successfully."))
                .andExpect(jsonPath("$.data.notification.notificationId").value(notificationId.toString()))
                .andExpect(jsonPath("$.data.notification.recipientUserId").value(recipientUserId.toString()))
                .andExpect(jsonPath("$.data.notification.status").value("PENDING"))
                .andExpect(jsonPath("$.data.notification.id").doesNotExist());

        verify(createNotificationUseCase).create(any(CreateNotificationRequest.class));
    }

    @Test
    void rejectsMissingRecipientUserId() throws Exception {
        CreateNotificationRequest request = validRequest();
        request.setRecipientUserId(null);

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.recipientUserId").value("Recipient user ID is required"));
    }

    @Test
    void rejectsBlankBody() throws Exception {
        CreateNotificationRequest request = validRequest();
        request.setBody("   ");

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.body").value("Body must not be blank"));
    }

    @Test
    void rejectsInvalidEnumValue() throws Exception {
        String payload = """
                {
                  "recipientUserId": "%s",
                  "type": "UNKNOWN",
                  "channel": "EMAIL",
                  "body": "A notification."
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request payload is not valid"));
    }

    @Test
    void rejectsInvalidJson() throws Exception {
        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request payload is not valid"));
    }

    private CreateNotificationRequest validRequest() {
        return new CreateNotificationRequest(
                UUID.randomUUID(),
                NotificationType.SYSTEM,
                NotificationChannel.IN_APP,
                "System notice",
                "A system notification."
        );
    }
}
