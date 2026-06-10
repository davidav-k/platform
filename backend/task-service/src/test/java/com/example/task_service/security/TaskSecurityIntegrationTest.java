package com.example.task_service.security;

import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.repository.TaskRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

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
    void regularUserListIncludesOwnedAndAssignedTasksOnly() throws Exception {
        UUID userId = UUID.randomUUID();
        TaskEntity created = saveTask("Created", UUID.randomUUID(), userId);
        TaskEntity assigned = saveTask("Assigned", userId, UUID.randomUUID());
        saveTask("Unrelated", UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(get("/api/v1/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(userId, "USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.items[*].taskId").value(containsInAnyOrder(
                created.getTaskId().toString(),
                assigned.getTaskId().toString()
            )));
    }

    @Test
    void regularUserCannotReadUnrelatedTask() throws Exception {
        TaskEntity unrelated = saveTask("Unrelated", UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(get("/api/v1/tasks/{taskId}", unrelated.getTaskId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(UUID.randomUUID(), "USER")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Task not found."));
    }

    @Test
    void adminCanListAndReadAllTasks() throws Exception {
        TaskEntity first = saveTask("First", UUID.randomUUID(), UUID.randomUUID());
        saveTask("Second", UUID.randomUUID(), UUID.randomUUID());
        UUID adminUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(adminUserId, "ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(2));

        mockMvc.perform(get("/api/v1/tasks/{taskId}", first.getTaskId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(adminUserId, "ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.task.taskId").value(first.getTaskId().toString()));
    }

    @Test
    void adminCanUpdateAnyTask() throws Exception {
        TaskEntity task = saveTask("Original", UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(patch("/api/v1/tasks/{taskId}", task.getTaskId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(UUID.randomUUID(), "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Admin update\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.task.title").value("Admin update"));
    }

    @Test
    void creatorCanUpdateOwnTask() throws Exception {
        UUID creatorUserId = UUID.randomUUID();
        TaskEntity task = saveTask("Original", UUID.randomUUID(), creatorUserId);
        UUID originalTaskId = task.getTaskId();

        mockMvc.perform(patch("/api/v1/tasks/{taskId}", task.getTaskId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(creatorUserId, "USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "priority": "HIGH",
                      "taskId": "%s",
                      "createdByUserId": "%s",
                      "createdAt": "2030-01-01T00:00:00Z",
                      "updatedAt": "2030-01-01T00:00:00Z",
                      "status": "DONE"
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.task.priority").value("HIGH"))
            .andExpect(jsonPath("$.data.task.taskId").value(originalTaskId.toString()))
            .andExpect(jsonPath("$.data.task.createdByUserId").value(creatorUserId.toString()))
            .andExpect(jsonPath("$.data.task.status").value("NEW"));

        TaskEntity updated = taskRepository.findByTaskId(originalTaskId).orElseThrow();
        assertThat(updated.getTaskId()).isEqualTo(originalTaskId);
        assertThat(updated.getCreatedByUserId()).isEqualTo(creatorUserId);
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.NEW);
    }

    @Test
    void assigneeCanUpdateAssignedTask() throws Exception {
        UUID assigneeUserId = UUID.randomUUID();
        TaskEntity task = saveTask("Original", assigneeUserId, UUID.randomUUID());

        mockMvc.perform(patch("/api/v1/tasks/{taskId}", task.getTaskId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(assigneeUserId, "USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Assignee update\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.task.description").value("Assignee update"));
    }

    @Test
    void unrelatedUserCannotUpdateTask() throws Exception {
        TaskEntity task = saveTask("Original", UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(patch("/api/v1/tasks/{taskId}", task.getTaskId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(UUID.randomUUID(), "USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Denied update\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Task not found."));
    }

    @Test
    void unauthenticatedListTasksReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
            .andExpect(status().isUnauthorized());
    }

    private String accessToken(UUID userId) {
        return accessToken(userId, "USER");
    }

    private String accessToken(UUID userId, String role) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("authorities", "document:create,document:read")
            .claim("role", role)
            .issuedAt(Date.from(Instant.now()))
            .notBefore(Date.from(Instant.now().minusSeconds(1)))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)), Jwts.SIG.HS512)
            .compact();
    }

    private TaskEntity saveTask(String title, UUID assigneeUserId, UUID createdByUserId) {
        return taskRepository.saveAndFlush(new TaskEntity(
            UUID.randomUUID(),
            title,
            null,
            TaskStatus.NEW,
            TaskPriority.MEDIUM,
            assigneeUserId,
            createdByUserId
        ));
    }
}
