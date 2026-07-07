package com.example.ai_service.outbox;

import com.example.ai_service.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class AiAuditPayloadFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AiAuditPayloadFactory payloadFactory = new AiAuditPayloadFactory(objectMapper);

    @Test
    void serializesOnlyAllowlistedAuditMetadata() throws Exception {
        UUID operationId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID actorUserId = UUID.fromString("20000000-0000-0000-0000-000000000002");

        String payload = payloadFactory.operationCompletedPayload(
                operationId,
                AiOutboxEventTypes.TASK_SUMMARIZED,
                new AuthenticatedUser(actorUserId, "user@example.com"),
                OffsetDateTime.parse("2026-07-06T10:15:30Z"),
                "temporary-noop",
                "not-configured"
        );

        JsonNode json = objectMapper.readTree(payload);
        Set<String> fields = StreamSupport.stream(
                        ((Iterable<String>) () -> json.fieldNames()).spliterator(),
                        false
                )
                .collect(Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder(
                "operationId",
                "operationType",
                "actorUserId",
                "actorEmail",
                "occurredAt",
                "providerName",
                "modelName"
        );
        assertThat(json.get("operationId").asText()).isEqualTo(operationId.toString());
        assertThat(json.get("actorUserId").asText()).isEqualTo(actorUserId.toString());
        assertThat(payload)
                .doesNotContain("title")
                .doesNotContain("description")
                .doesNotContain("prompt")
                .doesNotContain("generated")
                .doesNotContain("jwt")
                .doesNotContain("token");
    }
}
