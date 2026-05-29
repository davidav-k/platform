# Pull Request

## Summary
-

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
- [ ] No secrets, tokens, passwords, or private keys are committed
- [ ] Security-sensitive behavior was reviewed if JWT, cookies, auth filters, or permissions changed

## Validation
- [ ] `pre-commit run --all-files`
- [ ] `mvn -B -f backend/user-service/pom.xml test`
- [ ] `mvn -B -f infrastructure/api-gateway/pom.xml test`
- [ ] `mvn -B -f infrastructure/config-server/pom.xml test`
- [ ] `mvn -B -f infrastructure/eureka-server/pom.xml test`

## Notes for Reviewers
-
