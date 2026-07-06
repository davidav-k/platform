package com.example.ai_service.usecase;

import com.example.ai_service.enumeration.AiTaskPriority;
import com.example.ai_service.model.PrioritySuggestionRequest;
import com.example.ai_service.model.PrioritySuggestionResponse;
import com.example.ai_service.model.SubtaskSuggestionRequest;
import com.example.ai_service.model.SubtaskSuggestionResponse;
import com.example.ai_service.model.TaskDescriptionImprovementRequest;
import com.example.ai_service.model.TaskDescriptionImprovementResponse;
import com.example.ai_service.model.TaskSummaryRequest;
import com.example.ai_service.model.TaskSummaryResponse;
import com.example.ai_service.provider.AiProvider;
import com.example.ai_service.usecase.impl.AiTaskAssistanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiTaskAssistanceServiceTest {

    @Mock
    private AiProvider aiProvider;

    @InjectMocks
    private AiTaskAssistanceService aiTaskAssistanceService;

    @Test
    void improveTaskDescriptionDelegatesToProvider() {
        TaskDescriptionImprovementRequest request = new TaskDescriptionImprovementRequest(
                "Clarify onboarding",
                "Need better onboarding task details"
        );
        TaskDescriptionImprovementResponse expected =
                new TaskDescriptionImprovementResponse("Write detailed onboarding steps.");
        when(aiProvider.improveTaskDescription(request)).thenReturn(expected);

        TaskDescriptionImprovementResponse response =
                aiTaskAssistanceService.improveTaskDescription(request);

        assertThat(response).isEqualTo(expected);
        verify(aiProvider).improveTaskDescription(request);
    }

    @Test
    void suggestSubtasksDelegatesToProvider() {
        SubtaskSuggestionRequest request = new SubtaskSuggestionRequest(
                "Create audit page",
                "Add an audit log view for task events"
        );
        SubtaskSuggestionResponse expected = new SubtaskSuggestionResponse(List.of(
                "Define audit filters",
                "Render audit table"
        ));
        when(aiProvider.suggestSubtasks(request)).thenReturn(expected);

        SubtaskSuggestionResponse response = aiTaskAssistanceService.suggestSubtasks(request);

        assertThat(response).isEqualTo(expected);
        verify(aiProvider).suggestSubtasks(request);
    }

    @Test
    void summarizeTaskDelegatesToProvider() {
        TaskSummaryRequest request = new TaskSummaryRequest(
                "Refactor task validation",
                "Move duplicated validation into a focused service method"
        );
        TaskSummaryResponse expected = new TaskSummaryResponse("Refactor duplicated task validation.");
        when(aiProvider.summarizeTask(request)).thenReturn(expected);

        TaskSummaryResponse response = aiTaskAssistanceService.summarizeTask(request);

        assertThat(response).isEqualTo(expected);
        verify(aiProvider).summarizeTask(request);
    }

    @Test
    void suggestPriorityDelegatesToProvider() {
        PrioritySuggestionRequest request = new PrioritySuggestionRequest(
                "Fix login outage",
                "Users cannot authenticate after token rotation"
        );
        PrioritySuggestionResponse expected = new PrioritySuggestionResponse(
                AiTaskPriority.HIGH,
                "Authentication outage blocks users."
        );
        when(aiProvider.suggestPriority(request)).thenReturn(expected);

        PrioritySuggestionResponse response = aiTaskAssistanceService.suggestPriority(request);

        assertThat(response).isEqualTo(expected);
        verify(aiProvider).suggestPriority(request);
    }

    @Test
    void rejectsNullRequestBeforeCallingProvider() {
        assertThatThrownBy(() -> aiTaskAssistanceService.summarizeTask(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Summarize task request is required");

        verifyNoInteractions(aiProvider);
    }

    @Test
    void rejectsBlankTitleBeforeCallingProvider() {
        TaskSummaryRequest request = new TaskSummaryRequest("   ", "Summarize this task");

        assertThatThrownBy(() -> aiTaskAssistanceService.summarizeTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task title is required");

        verifyNoInteractions(aiProvider);
    }

    @Test
    void rejectsBlankDescriptionBeforeCallingProvider() {
        TaskSummaryRequest request = new TaskSummaryRequest("Summarize task", "   ");

        assertThatThrownBy(() -> aiTaskAssistanceService.summarizeTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task description is required");

        verifyNoInteractions(aiProvider);
    }

    @Test
    void rejectsNullRequestForSuggestPriorityBeforeCallingProvider() {
        assertThatThrownBy(() -> aiTaskAssistanceService.suggestPriority(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Suggest priority request is required");
        verifyNoInteractions(aiProvider);
    }
}
