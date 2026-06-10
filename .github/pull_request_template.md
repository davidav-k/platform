# Pull Request

## Summary
-

## Target Branch
- [ ] `dev`
- [ ] `main` (reason documented below)

## Scope
- [ ] Documentation or repository workflow only
- [ ] Backend service change
- [ ] Infrastructure/configuration change
- [ ] Frontend change

## MVP Stabilization Checks
- [ ] Controllers remain thin; business logic stays in services
- [ ] DTOs are used for API contracts; entities are not exposed directly
- [ ] No API contract changes unless explicitly documented
- [ ] No runtime dependency or platform version changes
- [ ] No `.env` file is committed
- [ ] No secrets, JWT tokens, passwords, private keys, API keys, or real credentials are committed
- [ ] `detect-private-key` and GitGuardian `ggshield` checks were run; any secret-scanning findings were resolved
- [ ] Security-sensitive behavior was reviewed if JWT, cookies, auth filters, or permissions changed
- [ ] Security and configuration impact is documented, or marked not applicable
- [ ] Documentation impact is addressed, or marked not applicable

## Validation
- [ ] `pre-commit run --all-files`
- [ ] Required GitHub Actions checks are passing
- [ ] `mvn -B -f backend/user-service/pom.xml test`
- [ ] `mvn -B -f infrastructure/api-gateway/pom.xml test`
- [ ] `mvn -B -f infrastructure/config-server/pom.xml test`
- [ ] `mvn -B -f infrastructure/eureka-server/pom.xml test`

## Notes for Reviewers
-
