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
| TD-007 | High | Gateway routing | The `/api/users/**` gateway route omits `PATCH`, so the implemented password-change endpoint is not externally reachable through the gateway. | Decide the intended external method policy and align gateway routing in a security-reviewed change. | Open |
| TD-008 | High | Authorization | `UserResource.enableMfa` references `#userId` in `@PreAuthorize`, but the method has no `userId` parameter. | Define owner authorization for MFA enrollment and correct the method-security expression with tests. | Open |
