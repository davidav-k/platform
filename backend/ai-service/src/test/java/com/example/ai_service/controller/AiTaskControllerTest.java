package com.example.ai_service.controller;

import com.example.ai_service.enumeration.AiTaskPriority;
import com.example.ai_service.exception.AiProviderException;
import com.example.ai_service.exception.AiProviderTimeoutException;
import com.example.ai_service.exception.AiProviderUnavailableException;
import com.example.ai_service.model.SuggestPriorityRequest;
import com.example.ai_service.model.SuggestPriorityResult;
import com.example.ai_service.model.SuggestSubtasksRequest;
import com.example.ai_service.model.SuggestSubtasksResult;
import com.example.ai_service.model.ImproveTaskDescriptionRequest;
import com.example.ai_service.model.ImproveTaskDescriptionResult;
import com.example.ai_service.model.SummarizeTaskRequest;
import com.example.ai_service.model.SummarizeTaskResult;
import com.example.ai_service.usecase.ImproveTaskDescriptionUseCase;
import com.example.ai_service.usecase.SuggestPriorityUseCase;
import com.example.ai_service.usecase.SuggestSubtasksUseCase;
import com.example.ai_service.usecase.SummarizeTaskUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
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
                "eureka.client.enabled=false"
        }
)
class AiTaskControllerTest {

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
    void improvesTaskDescription() throws Exception {
        when(improveTaskDescriptionUseCase.improveTaskDescription(any(ImproveTaskDescriptionRequest.class)))
                .thenReturn(new ImproveTaskDescriptionResult("Add acceptance criteria and rollout notes."));

        mockMvc.perform(post("/api/v1/ai/tasks/description/improve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.path").value("/api/v1/ai/tasks/description/improve"))
                .andExpect(jsonPath("$.message").value("Task description improved successfully."))
                .andExpect(jsonPath("$.data.result.improvedDescription")
                        .value("Add acceptance criteria and rollout notes."))
                .andExpect(jsonPath("$.data.result.provider").doesNotExist());

        verify(improveTaskDescriptionUseCase).improveTaskDescription(argThat(request ->
                "Improve onboarding".equals(request.title())
                        && "Make onboarding clearer.".equals(request.description())
        ));
    }

    @Test
    void suggestsSubtasks() throws Exception {
        when(suggestSubtasksUseCase.suggestSubtasks(any(SuggestSubtasksRequest.class)))
                .thenReturn(new SuggestSubtasksResult(List.of(
                        "Draft acceptance criteria",
                        "Review with product"
                )));

        mockMvc.perform(post("/api/v1/ai/tasks/subtasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Subtasks suggested successfully."))
                .andExpect(jsonPath("$.data.result.subtasks[0]").value("Draft acceptance criteria"))
                .andExpect(jsonPath("$.data.result.subtasks[1]").value("Review with product"));

        verify(suggestSubtasksUseCase).suggestSubtasks(any(SuggestSubtasksRequest.class));
    }

    @Test
    void summarizesTask() throws Exception {
        when(summarizeTaskUseCase.summarizeTask(any(SummarizeTaskRequest.class)))
                .thenReturn(new SummarizeTaskResult("Improve onboarding clarity."));

        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Task summarized successfully."))
                .andExpect(jsonPath("$.data.result.summary").value("Improve onboarding clarity."));

        verify(summarizeTaskUseCase).summarizeTask(any(SummarizeTaskRequest.class));
    }

    @Test
    void suggestsPriority() throws Exception {
        when(suggestPriorityUseCase.suggestPriority(any(SuggestPriorityRequest.class)))
                .thenReturn(new SuggestPriorityResult(AiTaskPriority.HIGH, "Blocks onboarding release."));

        mockMvc.perform(post("/api/v1/ai/tasks/priority/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Task priority suggested successfully."))
                .andExpect(jsonPath("$.data.result.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.result.reason").value("Blocks onboarding release."));

        verify(suggestPriorityUseCase).suggestPriority(any(SuggestPriorityRequest.class));
    }

    @Test
    void rejectsMissingTitle() throws Exception {
        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Make onboarding clearer."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Provided arguments are not valid"))
                .andExpect(jsonPath("$.data.title").value("Title must not be blank"));

        verifyNoInteractions(summarizeTaskUseCase);
    }

    @Test
    void rejectsBlankDescription() throws Exception {
        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Improve onboarding",
                                  "description": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.description").value("Description must not be blank"));

        verifyNoInteractions(summarizeTaskUseCase);
    }

    @Test
    void rejectsTitleLongerThanTwoHundredCharacters() throws Exception {
        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Make onboarding clearer."
                                }
                                """.formatted("a".repeat(201))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.title").value("Title must not exceed 200 characters"));

        verifyNoInteractions(summarizeTaskUseCase);
    }

    @Test
    void rejectsDescriptionLongerThanFiveThousandCharacters() throws Exception {
        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Improve onboarding",
                                  "description": "%s"
                                }
                                """.formatted("a".repeat(5001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.description").value("Description must not exceed 5000 characters"));

        verifyNoInteractions(summarizeTaskUseCase);
    }

    @Test
    void rejectsInvalidJson() throws Exception {
        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request payload is not valid"));

        verifyNoInteractions(summarizeTaskUseCase);
    }

    @Test
    void providerUnavailableReturnsServiceUnavailable() throws Exception {
        when(summarizeTaskUseCase.summarizeTask(any(SummarizeTaskRequest.class)))
                .thenThrow(new AiProviderUnavailableException("AI provider is unavailable"));

        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("AI provider is unavailable"));
    }

    @Test
    void providerTimeoutReturnsGatewayTimeout() throws Exception {
        when(summarizeTaskUseCase.summarizeTask(any(SummarizeTaskRequest.class)))
                .thenThrow(new AiProviderTimeoutException("AI provider timed out"));

        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value(504))
                .andExpect(jsonPath("$.message").value("AI provider request timed out"));
    }

    @Test
    void providerFailureReturnsBadGateway() throws Exception {
        when(summarizeTaskUseCase.summarizeTask(any(SummarizeTaskRequest.class)))
                .thenThrow(new AiProviderException("AI provider failed"));

        mockMvc.perform(post("/api/v1/ai/tasks/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(502))
                .andExpect(jsonPath("$.message").value("AI provider request failed"));
    }

    private ValidTaskPayload validPayload() {
        return new ValidTaskPayload("Improve onboarding", "Make onboarding clearer.");
    }

    private record ValidTaskPayload(String title, String description) {
    }
}