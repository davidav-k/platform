# AI Service Audit Flow

```mermaid
sequenceDiagram
    autonumber
    participant Frontend
    participant Gateway as API Gateway
    participant AI as AI Service
    participant Provider as AiProvider
    participant Outbox as AI outbox_events
    participant Kafka as Kafka platform.ai-events
    participant Audit as Audit Service
    participant AuditDb as audit_records

    Frontend->>Gateway: POST /api/ai/tasks/{operation}
    Gateway->>Gateway: Validate JWT early
    Gateway->>AI: POST /api/v1/ai/tasks/{operation}
    AI->>AI: Validate JWT, role, title, description
    AI->>Provider: Execute task assistance operation
    Provider-->>AI: Structured result
    AI->>Outbox: Save AI_OPERATION event in transaction
    AI-->>Gateway: 200 response with data.result
    Gateway-->>Frontend: AI result
    Outbox-->>Kafka: Publish AI event envelope
    Kafka-->>Audit: Consume AI event
    Audit->>Audit: Normalize and sanitize payload
    Audit->>AuditDb: Persist audit record idempotently
```

The current `dev` and `test` profiles use `TemporaryNoOpAiProvider` when no
other `AiProvider` bean exists. This checkout does not include an Ollama
provider implementation; replace the `Provider` participant with Ollama only
after code introduces a concrete Ollama-backed provider and configuration.