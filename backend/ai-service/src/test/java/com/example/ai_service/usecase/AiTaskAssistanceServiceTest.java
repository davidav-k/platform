package com.example.ai_service.usecase;

import com.example.ai_service.enumeration.AiTaskPriority;
import com.example.ai_service.model.SuggestPriorityRequest;
import com.example.ai_service.model.SuggestPriorityResult;
import com.example.ai_service.model.SuggestSubtasksRequest;
import com.example.ai_service.model.SuggestSubtasksResult;
import com.example.ai_service.model.ImproveTaskDescriptionRequest;
import com.example.ai_service.model.ImproveTaskDescriptionResult;
import com.example.ai_service.model.SummarizeTaskRequest;
import com.example.ai_service.model.SummarizeTaskResult;
import com.example.ai_service.outbox.AiOperationAuditRecorder;
import com.example.ai_service.outbox.AiOutboxEventTypes;
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

    @Mock
    private AiOperationAuditRecorder auditRecorder;

    @InjectMocks
    private AiTaskAssistanceService aiTaskAssistanceService;

    @Test
    void improveTaskDescriptionDelegatesToProvider() {
        ImproveTaskDescriptionRequest request = new ImproveTaskDescriptionRequest(
                "Clarify onboarding",
                "Need better onboarding task details"
        );
        ImproveTaskDescriptionResult expected =
                new ImproveTaskDescriptionResult("Write detailed onboarding steps.");
        when(aiProvider.improveTaskDescription(request)).thenReturn(expected);

        ImproveTaskDescriptionResult response =
                aiTaskAssistanceService.improveTaskDescription(request);

        assertThat(response).isEqualTo(expected);
        verify(aiProvider).improveTaskDescription(request);
        verify(auditRecorder).recordSuccess(AiOutboxEventTypes.TASK_DESCRIPTION_IMPROVED);
    }

    @Test
    void suggestSubtasksDelegatesToProvider() {
        SuggestSubtasksRequest request = new SuggestSubtasksRequest(
                "Create audit page",
                "Add an audit log view for task events"
        );
        SuggestSubtasksResult expected = new SuggestSubtasksResult(List.of(
                "Define audit filters",
                "Render audit table"
        ));
        when(aiProvider.suggestSubtasks(request)).thenReturn(expected);

        SuggestSubtasksResult response = aiTaskAssistanceService.suggestSubtasks(request);

        assertThat(response).isEqualTo(expected);
        verify(aiProvider).suggestSubtasks(request);
        verify(auditRecorder).recordSuccess(AiOutboxEventTypes.SUBTASKS_SUGGESTED);
    }

    @Test
    void summarizeTaskDelegatesToProvider() {
        SummarizeTaskRequest request = new SummarizeTaskRequest(
                "Refactor task validation",
                "Move duplicated validation into a focused service method"
        );
        SummarizeTaskResult expected = new SummarizeTaskResult("Refactor duplicated task validation.");
        when(aiProvider.summarizeTask(request)).thenReturn(expected);

        SummarizeTaskResult response = aiTaskAssistanceService.summarizeTask(request);

        assertThat(response).isEqualTo(expected);
        verify(aiProvider).summarizeTask(request);
        verify(auditRecorder).recordSuccess(AiOutboxEventTypes.TASK_SUMMARIZED);
    }

    @Test
    void suggestPriorityDelegatesToProvider() {
        SuggestPriorityRequest request = new SuggestPriorityRequest(
                "Fix login outage",
                "Users cannot authenticate after token rotation"
        );
        SuggestPriorityResult expected = new SuggestPriorityResult(
                AiTaskPriority.HIGH,
                "Authentication outage blocks users."
        );
        when(aiProvider.suggestPriority(request)).thenReturn(expected);

        SuggestPriorityResult response = aiTaskAssistanceService.suggestPriority(request);

        assertThat(response).isEqualTo(expected);
        verify(aiProvider).suggestPriority(request);
        verify(auditRecorder).recordSuccess(AiOutboxEventTypes.PRIORITY_SUGGESTED);
    }

    @Test
    void doesNotRecordEventWhenProviderFails() {
        SummarizeTaskRequest request = new SummarizeTaskRequest(
                "Summarize task",
                "Task description"
        );
        when(aiProvider.summarizeTask(request)).thenThrow(new IllegalStateException("provider failed"));

        assertThatThrownBy(() -> aiTaskAssistanceService.summarizeTask(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("provider failed");

        verifyNoInteractions(auditRecorder);
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
        SummarizeTaskRequest request = new SummarizeTaskRequest("   ", "Summarize this task");

        assertThatThrownBy(() -> aiTaskAssistanceService.summarizeTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task title is required");

        verifyNoInteractions(aiProvider);
    }

    @Test
    void rejectsBlankDescriptionBeforeCallingProvider() {
        SummarizeTaskRequest request = new SummarizeTaskRequest("Summarize task", "   ");

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
