# Technical Debt

| Issue | Impact | Priority | Planned Fix | Status |
| --- | --- | --- | --- | --- |
| No root Maven aggregator POM | CI matrix must be updated manually when services are added | High | Keep CI matrix updated, or introduce an aggregator POM when the build layout stabilizes | Open |
| GitHub secret scanning and push protection not confirmed | Secrets may be exposed without detection | High | Enable GitHub secret scanning, push protection, and GitGuardian GitHub integration in repository settings | Open |
| Branch protection rules not configured | Unreviewed merges to `main` and `dev` are possible | Medium | Configure GitHub branch protection and document the expected target branch for MVP work | Open |
| User deletion is a hard delete with no cross-service cleanup | Dependent data in future services will not be cleaned up | Medium | Define retention and cross-service deletion behavior before adding dependent services | Open |
