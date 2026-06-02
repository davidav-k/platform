# Technical Debt

This file tracks known repository and workflow debt during MVP stabilization. Keep entries short and actionable.

| ID | Priority | Area | Debt | Suggested Action | Status |
| --- | --- | --- | --- | --- | --- |
| TD-001 | High | CI | Maven projects are independent and there is no root aggregator POM. | Keep CI matrix updated when services are added, or introduce an aggregator POM later if the build layout stabilizes. | Open |
| TD-002 | High | Repository security | GitGuardian is configured locally, but repository-level GitHub secret scanning and push protection must be confirmed manually. | Enable GitHub secret scanning, push protection, and GitGuardian GitHub integration in repository settings. | Open |
| TD-003 | Medium | Secrets | `.env.example` previously used realistic placeholder values such as `Password123` and a concrete-looking JWT secret. | Keep example credentials clearly fake and development-only. | Resolved |
| TD-004 | Medium | Documentation | Root README and service READMEs had startup inconsistencies and overstated planned components. | Keep the documentation audit current as services become implemented. | Resolved |
| TD-005 | Medium | Branching | Existing branches include `main`, `dev`, and older feature branches, but branch protection and default PR target rules are not visible in the repository. | Configure GitHub branch protection and document the expected target branch for MVP work. | Open |
| TD-006 | Low | Repository hygiene | macOS `.DS_Store` files exist as untracked files in the working tree. | Ignore `.DS_Store` and remove local copies when convenient. | Open |
| TD-007 | High | Gateway routing | The `/api/users/**` gateway route previously omitted `PATCH`. | `PATCH` routing and CORS support were added for the existing password-change endpoint. | Resolved |
| TD-008 | High | Authorization | MFA enrollment previously referenced a missing `#userId` parameter and accepted an arbitrary target email. | Enrollment now derives the email from the authenticated principal and is self-service only. | Resolved |
| TD-009 | Medium | Account lifecycle | User deletion is an MVP hard delete. Soft deletion, retention policy, and future cross-service cleanup are not implemented. | Define retention and cross-service account-deletion behavior before adding dependent services. | Open |
