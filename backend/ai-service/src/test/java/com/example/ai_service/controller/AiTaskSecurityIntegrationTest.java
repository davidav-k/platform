package com.example.ai_service.controller;

import com.example.ai_service.model.SummarizeTaskRequest;
import com.example.ai_service.model.SummarizeTaskResult;
import com.example.ai_service.security.AiAccessDeniedHandler;
import com.example.ai_service.security.AiAuthenticationEntryPoint;
import com.example.ai_service.security.JwtAuthenticationFilter;
import com.example.ai_service.security.JwtTokenService;
import com.example.ai_service.security.SecurityConfig;
import com.example.ai_service.usecase.ImproveTaskDescriptionUseCase;
import com.example.ai_service.usecase.SuggestPriorityUseCase;
import com.example.ai_service.usecase.SuggestSubtasksUseCase;
import com.example.ai_service.usecase.SummarizeTaskUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AiTaskController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "jwt.secret=" + AiTaskSecurityIntegrationTest.TEST_SECRET
        }
)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenService.class,
        AiAuthenticationEntryPoint.class,
        AiAccessDeniedHandler.class
})
class AiTaskSecurityIntegrationTest {

    static final String TEST_SECRET =
            "QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQQ==";

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ImproveTaskDescriptionUseCase improveTaskDescriptionUseCase;

    @MockitoBean
    private SuggestSubtasksUseCase suggestSubtasksUseCase;

    @MockitoBean
    private SummarizeTaskUseCase summarizeTaskUseCase;

    @MockitoBean
    private SuggestPriorityUseCase suggestPriorityUseCase;

    @Test
    void authenticatedAccessWithBearerTokenSucceeds() throws Exception {
        when(summarizeTaskUseCase.summarizeTask(any(SummarizeTaskRequest.class)))
                .thenReturn(new SummarizeTaskResult("Improve onboarding clarity."));

        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Task summarized successfully."))
                .andExpect(jsonPath("$.data.result.summary").value("Improve onboarding clarity."));

        verify(summarizeTaskUseCase).summarizeTask(any(SummarizeTaskRequest.class));
    }

    @Test
    void successfulAuthorizedRequestWithAdminRoleSucceeds() throws Exception {
        when(summarizeTaskUseCase.summarizeTask(any(SummarizeTaskRequest.class)))
                .thenReturn(new SummarizeTaskResult("Improve onboarding clarity."));

        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void accessTokenCookieAuthenticatesRequest() throws Exception {
        when(summarizeTaskUseCase.summarizeTask(any(SummarizeTaskRequest.class)))
                .thenReturn(new SummarizeTaskResult("Improve onboarding clarity."));

        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .cookie(new Cookie("access-token", token("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void unauthenticatedAccessReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required"));

        verifyNoInteractions(summarizeTaskUseCase);
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required"));

        verifyNoInteractions(summarizeTaskUseCase);
    }

    @Test
    void authenticatedTokenWithoutAllowedRoleReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("GUEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));

        verifyNoInteractions(summarizeTaskUseCase);
    }

    private String token(String role) {
        return Jwts.builder()
                .subject(USER_ID.toString())
                .claim("role", role)
                .claim("authorities", "document:read")
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)), Jwts.SIG.HS512)
                .compact();
    }

    private Map<String, String> validPayload() {
        return Map.of(
                "title", "Improve onboarding",
                "description", "Make onboarding clearer."
        );
    }
}