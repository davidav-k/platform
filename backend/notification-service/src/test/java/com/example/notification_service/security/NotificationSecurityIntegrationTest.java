package com.example.notification_service.security;

import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.repository.NotificationRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "jwt.secret=" + NotificationSecurityIntegrationTest.TEST_SECRET
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class NotificationSecurityIntegrationTest {

    static final String TEST_SECRET =
            "QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQQ==";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
            .withDatabaseName("notification_service_security_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void clearNotifications() {
        notificationRepository.deleteAll();
    }

    @Test
    void authenticatedCreateSucceedsAndPersistsNotification() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientUserId": "%s",
                                  "type": "SYSTEM",
                                  "channel": "IN_APP",
                                  "subject": "Secure notification",
                                  "body": "Created with a signed JWT"
                                }
                                """.formatted(recipientUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.notification.recipientUserId").value(recipientUserId.toString()));

        NotificationEntity persisted = notificationRepository.findAll().stream()
                .filter(notification -> "Secure notification".equals(notification.getSubject()))
                .findFirst()
                .orElseThrow();
        assertThat(persisted.getRecipientUserId()).isEqualTo(recipientUserId);
    }

    @Test
    void authenticatedInternalSystemCreatePersistsSourceMetadata() throws Exception {
        UUID recipientUserId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        mockMvc.perform(post("/internal/api/v1/notifications/system")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientUserId": "%s",
                                  "type": "TASK_ASSIGNED",
                                  "title": "New task assigned",
                                  "message": "Task \\\"Implement login\\\" was assigned to you",
                                  "sourceService": "task-service",
                                  "sourceEntityType": "TASK",
                                  "sourceEntityId": "%s"
                                }
                                """.formatted(recipientUserId, taskId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.notification.recipientUserId").value(recipientUserId.toString()))
                .andExpect(jsonPath("$.data.notification.type").value("TASK_ASSIGNED"));

        NotificationEntity persisted = notificationRepository.findAll().stream()
                .filter(notification -> taskId.equals(notification.getSourceEntityId()))
                .findFirst()
                .orElseThrow();
        assertThat(persisted.getSourceService()).isEqualTo("task-service");
        assertThat(persisted.getSourceEntityType()).isEqualTo("TASK");
        assertThat(persisted.getChannel().name()).isEqualTo("IN_APP");
    }

    @Test
    void unauthenticatedInternalSystemCreateReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/internal/api/v1/notifications/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedListSucceeds() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void unauthenticatedListReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void invalidBearerTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredBearerTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validAccessTokenCookieSucceeds() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .cookie(new Cookie("access-token", accessToken(UUID.randomUUID()))))
                .andExpect(status().isOk());
    }

    @Test
    void bearerTokenTakesPrecedenceOverCookie() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .cookie(new Cookie("access-token", accessToken(UUID.randomUUID()))))
                .andExpect(status().isUnauthorized());
    }

    private String accessToken(UUID userId) {
        Instant now = Instant.now();
        return signedToken(userId, now.minusSeconds(1), now.plusSeconds(600));
    }

    private String expiredToken(UUID userId) {
        Instant now = Instant.now();
        return signedToken(userId, now.minusSeconds(600), now.minusSeconds(1));
    }

    private String signedToken(UUID userId, Instant notBefore, Instant expiration) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("authorities", "notification:create,notification:read")
                .claim("role", "USER")
                .issuedAt(Date.from(notBefore))
                .notBefore(Date.from(notBefore))
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)), Jwts.SIG.HS512)
                .compact();
    }
}
