package com.example.ai_service.provider;

import com.example.ai_service.model.SuggestSubtasksRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemporaryNoOpAiProviderTest {

    private final TemporaryNoOpAiProvider provider = new TemporaryNoOpAiProvider();

    @Test
    void suggestSubtasksReturnsDeterministicNonEmptyFallback() {
        var request = new SuggestSubtasksRequest("Improve onboarding", "Make onboarding clearer.");

        var result = provider.suggestSubtasks(request);

        assertThat(result.subtasks())
                .containsExactly("Clarify acceptance criteria", "Identify implementation steps");
    }
}