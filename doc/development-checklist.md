# Development Checklist

Use this checklist before opening a pull request.

## Scope

- [ ] The change supports MVP stabilization.
- [ ] The change is limited to the smallest practical scope.
- [ ] No unrelated refactoring is included.
- [ ] No business logic, API contract, framework version, or runtime dependency changes are included unless they are the stated purpose of the PR.

## Security

- [ ] No JWT tokens, passwords, private keys, secrets, or generated credentials are committed.
- [ ] JWT validation, expiration, refresh flow, cookies, and filter order were reviewed if authentication changed.
- [ ] Sensitive values are read from configuration or environment variables.
- [ ] Logs do not include tokens, passwords, or secrets.

## Service Design

- [ ] Controllers validate input, call services, and return DTOs.
- [ ] Business rules and transactions stay in the service layer.
- [ ] Entities, DTOs, and security principals are not mixed.
- [ ] Each microservice owns its own data.

## Validation

- [ ] `pre-commit run --all-files`
- [ ] `mvn -B -f backend/user-service/pom.xml test`
- [ ] `mvn -B -f infrastructure/api-gateway/pom.xml test`
- [ ] `mvn -B -f infrastructure/config-server/pom.xml test`
- [ ] `mvn -B -f infrastructure/eureka-server/pom.xml test`
- [ ] `./scripts/check-local-stack.sh` when local startup behavior is affected
- [ ] Startup instructions were checked if Docker, config, ports, or service discovery changed.

## Documentation

- [ ] README or `doc/` was updated for architecture, API contract, startup, or workflow changes.
- [ ] New technical debt was added to `doc/technical-debt.md` if it cannot be addressed in the current PR.
