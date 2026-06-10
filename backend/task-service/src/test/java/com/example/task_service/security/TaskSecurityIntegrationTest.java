package com.example.task_service.security;

import com.example.task_service.entity.TaskEntity;
import com.example.task_service.repository.TaskRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:task_security_integration_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "jwt.secret=" + TaskSecurityIntegrationTest.TEST_SECRET
})
@AutoConfigureMockMvc
class TaskSecurityIntegrationTest {

    static final String TEST_SECRET =
        "QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQQ==";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void authenticatedCreateStoresTokenSubjectAsCreator() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = accessToken(userId);

        mockMvc.perform(post("/api/v1/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Secure task creation",
                      "description": "Created with a signed JWT",
                      "priority": "HIGH",
                      "createdByUserId": "00000000-0000-0000-0000-000000000001"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.task.createdByUserId").value(userId.toString()));

        TaskEntity persisted = taskRepository.findAll().stream()
            .filter(task -> "Secure task creation".equals(task.getTitle()))
            .findFirst()
            .orElseThrow();

        assertThat(persisted.getCreatedByUserId()).isEqualTo(userId);
    }

    @Test
    void authenticatedListTasksSucceeds() throws Exception {
        mockMvc.perform(get("/api/v1/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void unauthenticatedListTasksReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
            .andExpect(status().isUnauthorized());
    }

    private String accessToken(UUID userId) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("authorities", "document:create,document:read")
            .claim("role", "USER")
            .issuedAt(Date.from(Instant.now()))
            .notBefore(Date.from(Instant.now().minusSeconds(1)))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)), Jwts.SIG.HS512)
            .compact();
    }
}
