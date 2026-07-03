# Audit Log Read Flow

```mermaid
sequenceDiagram
    actor Admin
    participant UI as Vue Audit Log
    participant Gateway as API Gateway
    participant Audit as Audit Service
    participant DB as audits_db

    Admin->>UI: Open /audit
    UI->>Gateway: GET /api/audit?filters&page&size&sort
    Gateway->>Audit: GET /api/v1/audit?filters&page&size&sort
    Audit->>Audit: Authenticate and require admin role
    Audit->>DB: Query audit_records
    DB-->>Audit: Read-only page
    Audit-->>Gateway: Audit response envelope
    Gateway-->>UI: Audit records and page metadata
    UI-->>Admin: Table, filters, and pagination

    Admin->>UI: Open an audit record
    UI->>Gateway: GET /api/audit/{auditId}
    Gateway->>Audit: GET /api/v1/audit/{auditId}
    Audit->>DB: Find by public auditId
    DB-->>Audit: Audit record
    Audit-->>UI: Read-only audit details
```

The UI never exposes the database primary key and does not provide audit write,
delete, replay, or export actions. Audit Service remains authoritative for
admin authorization.
