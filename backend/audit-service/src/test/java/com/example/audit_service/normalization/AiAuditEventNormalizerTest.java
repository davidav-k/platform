package com.example.audit_service.normalization;

import com.example.audit_service.kafka.KafkaOutboxEventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiAuditEventNormalizerTest {

    private static final UUID EVENT_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OPERATION_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID ACTOR_USER_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final OffsetDateTime OCCURRED_AT =
            OffsetDateTime.parse("2026-07-06T10:15:30Z");

    private AiAuditEventNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new AiAuditEventNormalizer(new ObjectMapper().findAndRegisterModules());
    }

    @ParameterizedTest
    @CsvSource({
            "AI_TASK_DESCRIPTION_IMPROVED, IMPROVE_TASK_DESCRIPTION",
            "AI_SUBTASKS_SUGGESTED, SUGGEST_SUBTASKS",
            "AI_TASK_SUMMARIZED, SUMMARIZE_TASK",
            "AI_PRIORITY_SUGGESTED, SUGGEST_PRIORITY"
    })
    void normalizesSupportedAiEvents(String eventType, String expectedAction) {
        NormalizedAuditEvent normalized = normalizer.normalize(event(eventType, payload())).orElseThrow();

        assertThat(normalized.eventId()).isEqualTo(EVENT_ID);
        assertThat(normalized.aggregateType()).isEqualTo("AI_OPERATION");
        assertThat(normalized.aggregateId()).isEqualTo(OPERATION_ID);
        assertThat(normalized.sourceService()).isEqualTo("ai-service");
        assertThat(normalized.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(normalized.actorEmail()).isNull();
        assertThat(normalized.action()).isEqualTo(expectedAction);
        assertThat(normalized.payload())
                .contains("operationId", "actorUserId", "providerName", "modelName")
                .doesNotContain("actorEmail", "title", "description", "prompt", "generated");
        assertThat(normalized.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void removesFieldsOutsideTheAiAuditAllowlist() {
        String payload = """
                {"operationId":"%s","operationType":"AI_TASK_SUMMARIZED",
                 "actorUserId":"%s",
                 "occurredAt":"2026-07-06T10:15:30Z",
                 "providerName":"temporary-noop","modelName":"not-configured",
                 "prompt":"secret prompt","summary":"generated response","jwt":"secret token"}
                """.formatted(OPERATION_ID, ACTOR_USER_ID);

        NormalizedAuditEvent normalized =
                normalizer.normalize(event("AI_TASK_SUMMARIZED", payload)).orElseThrow();

        assertThat(normalized.payload())
                .doesNotContain("secret prompt")
                .doesNotContain("generated response")
                .doesNotContain("secret token")
                .doesNotContain("prompt")
                .doesNotContain("summary")
                .doesNotContain("jwt");
    }

    @Test
    void ignoresUnsupportedAiEvent() {
        assertThat(normalizer.normalize(event("AI_UNKNOWN", payload()))).isEmpty();
    }

    @Test
    void rejectsMalformedPayload() {
        assertThatThrownBy(() -> normalizer.normalize(event("AI_TASK_SUMMARIZED", "not-json")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kafka AI event payload is not valid");
    }

    private KafkaOutboxEventMessage event(String eventType, String payload) {
        return new KafkaOutboxEventMessage(
                EVENT_ID,
                eventType,
                "AI_OPERATION",
                OPERATION_ID,
                OCCURRED_AT,
                1,
                payload
        );
    }

    private String payload() {
        return """
                {"operationId":"%s","operationType":"AI_TASK_SUMMARIZED",
                 "actorUserId":"%s",
                 "occurredAt":"2026-07-06T10:15:30Z",
                 "providerName":"temporary-noop","modelName":"not-configured"}
                """.formatted(OPERATION_ID, ACTOR_USER_ID).strip();
    }
}
