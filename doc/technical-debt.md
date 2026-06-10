# Technical Debt

| Issue | Impact | Priority | Planned Fix | Status |
| --- | --- | --- | --- | --- |
| No root Maven aggregator POM | Maven modules must be invoked independently and CI coverage must be maintained explicitly | Low | Consider an aggregator POM only when the build layout stabilizes | Open |
| Incomplete CI matrix | CI did not execute task-service or notification-service tests | High | Added every currently buildable Maven module to the CI matrix | Resolved |
| Testcontainers 1.x requires Docker API compatibility override with Docker Engine 29 | Backend integration tests depend on `api.version=1.44` in Maven Surefire | Medium | Upgrade Testcontainers when the Spring Boot dependency baseline supports a compatible release, then remove the override after verification | Open |
| GitHub secret scanning and push protection not confirmed | Repository hooks reduce local risk, but GitHub may still accept exposed secrets if repository protections are disabled | High | Required settings and local controls are documented; a repository administrator must verify secret scanning and push protection in GitHub | Partially mitigated |
| Branch protection rules not confirmed | Unreviewed merges or merges without passing CI may be possible | Medium | Required branch protection and CI checks are documented; a repository administrator must configure and verify them in GitHub | Partially mitigated |
| User deletion is a hard delete with no cross-service cleanup | Dependent data in future services will not be cleaned up | Medium | Define retention and cross-service deletion behavior before adding dependent services | Open |
| task-service MVP scope: no update, delete, assign, status-change, or comment endpoints | Task lifecycle management is incomplete | Medium | Implement remaining task endpoints as next MVP milestone | Open |
| task-service has no role-based authorization | All authenticated users can create and read all tasks regardless of role | Medium | Add role and ownership checks to task endpoints before any user-facing release | Open |
| Synchronous task-to-notification REST call is MVP-only | Notification delivery is best-effort and has no durable retry or atomic handoff | Medium | Replace with an event-driven outbox flow and add service-to-service authentication | Open |
