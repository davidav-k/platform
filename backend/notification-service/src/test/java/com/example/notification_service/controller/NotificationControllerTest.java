package com.example.notification_service.controller;

import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.dto.NotificationListQuery;
import com.example.notification_service.dto.NotificationListResponse;
import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.dto.PageResponse;
import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;
import com.example.notification_service.exception.NotificationNotFoundException;
import com.example.notification_service.security.JwtAuthenticationFilter;
import com.example.notification_service.security.JwtTokenService;
import com.example.notification_service.security.SecurityConfig;
import com.example.notification_service.usecase.CreateNotificationUseCase;
import com.example.notification_service.usecase.GetNotificationUseCase;
import com.example.notification_service.usecase.ListNotificationsUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
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
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@WithMockUser
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateNotificationUseCase createNotificationUseCase;

    @MockitoBean
    private GetNotificationUseCase getNotificationUseCase;

    @MockitoBean
    private ListNotificationsUseCase listNotificationsUseCase;

    @MockitoBean
    private JwtTokenService jwtTokenService;

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
                .andExpect(header().string("Location", "/api/v1/notifications/" + notificationId))
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

    @Test
    void getsNotificationByPublicId() throws Exception {
        UUID notificationId = UUID.randomUUID();
        NotificationResponse notification = response(notificationId);
        when(getNotificationUseCase.getByNotificationId(notificationId)).thenReturn(notification);

        mockMvc.perform(get("/api/v1/notifications/{notificationId}", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification retrieved successfully."))
                .andExpect(jsonPath("$.data.notification.notificationId").value(notificationId.toString()))
                .andExpect(jsonPath("$.data.notification.id").doesNotExist());

        verify(getNotificationUseCase).getByNotificationId(notificationId);
    }

    @Test
    void missingNotificationReturnsNotFound() throws Exception {
        UUID notificationId = UUID.randomUUID();
        when(getNotificationUseCase.getByNotificationId(notificationId))
                .thenThrow(new NotificationNotFoundException(notificationId));

        mockMvc.perform(get("/api/v1/notifications/{notificationId}", notificationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Notification not found."));
    }

    @Test
    void invalidNotificationIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/{notificationId}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Provided arguments are not valid"))
                .andExpect(jsonPath("$.data.notificationId").value("Value has an invalid format"));
    }

    @Test
    void listsNotificationsWithItemsAndPage() throws Exception {
        UUID notificationId = UUID.randomUUID();
        when(listNotificationsUseCase.list(any(NotificationListQuery.class)))
                .thenReturn(new NotificationListResponse(
                        List.of(response(notificationId)),
                        new PageResponse(0, 20, 1, 1)
                ));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notifications retrieved successfully."))
                .andExpect(jsonPath("$.data.items[0].notificationId").value(notificationId.toString()))
                .andExpect(jsonPath("$.data.items[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.page.number").value(0))
                .andExpect(jsonPath("$.data.page.size").value(20))
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.page.totalPages").value(1));
    }

    @Test
    void listPassesFiltersToUseCase() throws Exception {
        UUID recipientUserId = UUID.randomUUID();
        when(listNotificationsUseCase.list(any(NotificationListQuery.class)))
                .thenReturn(new NotificationListResponse(List.of(), new PageResponse(1, 10, 0, 0)));

        mockMvc.perform(get("/api/v1/notifications")
                        .param("recipientUserId", recipientUserId.toString())
                        .param("status", "SENT")
                        .param("channel", "EMAIL")
                        .param("type", "TASK_ASSIGNED")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "updatedAt,asc"))
                .andExpect(status().isOk());

        verify(listNotificationsUseCase).list(argThat(query ->
                recipientUserId.equals(query.getRecipientUserId())
                        && query.getStatus() == NotificationStatus.SENT
                        && query.getChannel() == NotificationChannel.EMAIL
                        && query.getType() == NotificationType.TASK_ASSIGNED
                        && query.getPage() == 1
                        && query.getSize() == 10
                        && "updatedAt,asc".equals(query.getSort())
        ));
    }

    @Test
    void invalidStatusReturnsBadRequest() throws Exception {
        assertInvalidQueryParameter("status", "UNKNOWN", "status");
    }

    @Test
    void invalidChannelReturnsBadRequest() throws Exception {
        assertInvalidQueryParameter("channel", "SMS", "channel");
    }

    @Test
    void invalidTypeReturnsBadRequest() throws Exception {
        assertInvalidQueryParameter("type", "UNKNOWN", "type");
    }

    @Test
    void invalidPageReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/notifications").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Provided arguments are not valid"));
    }

    @Test
    void invalidSizeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/notifications").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Provided arguments are not valid"));
    }

    @Test
    void unauthenticatedCreateReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/notifications")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void unauthenticatedListReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications").with(anonymous()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void unauthenticatedGetReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/{notificationId}", UUID.randomUUID()).with(anonymous()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
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

    private NotificationResponse response(UUID notificationId) {
        return new NotificationResponse(
                notificationId,
                UUID.randomUUID(),
                NotificationType.SYSTEM,
                NotificationChannel.IN_APP,
                "System notice",
                "A system notification.",
                NotificationStatus.PENDING,
                OffsetDateTime.parse("2026-06-08T10:00:00Z"),
                OffsetDateTime.parse("2026-06-08T10:00:00Z"),
                null,
                null
        );
    }

    private void assertInvalidQueryParameter(String name, String value, String errorField) throws Exception {
        mockMvc.perform(get("/api/v1/notifications").param(name, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Provided arguments are not valid"))
                .andExpect(jsonPath("$.data." + errorField).value("Value has an invalid format"));
    }
}
