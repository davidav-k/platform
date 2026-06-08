# Technical Debt

| Issue | Impact | Priority | Planned Fix | Status |
| --- | --- | --- | --- | --- |
| No root Maven aggregator POM | CI matrix must be updated manually when services are added | High | Keep CI matrix updated, or introduce an aggregator POM when the build layout stabilizes | Open |
| GitHub secret scanning and push protection not confirmed | Secrets may be exposed without detection | High | Enable GitHub secret scanning, push protection, and GitGuardian GitHub integration in repository settings | Open |
| Branch protection rules not configured | Unreviewed merges to `main` and `dev` are possible | Medium | Configure GitHub branch protection and document the expected target branch for MVP work | Open |
| User deletion is a hard delete with no cross-service cleanup | Dependent data in future services will not be cleaned up | Medium | Define retention and cross-service deletion behavior before adding dependent services | Open |
| task-service MVP scope: no update, delete, assign, status-change, or comment endpoints | Task lifecycle management is incomplete | Medium | Implement remaining task endpoints as next MVP milestone | Open |
| task-service has no role-based authorization | All authenticated users can create and read all tasks regardless of role | Medium | Add role and ownership checks to task endpoints before any user-facing release | Open |
| Synchronous task-to-notification REST call is MVP-only | Notification delivery is best-effort and has no durable retry or atomic handoff | Medium | Replace with an event-driven outbox flow and add service-to-service authentication | Open |
