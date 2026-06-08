# Development Checklist

## Before Commit

- [ ] No JWT tokens, passwords, private keys, secrets, or generated credentials are committed.
- [ ] Sensitive values are read from configuration or environment variables.
- [ ] Logs do not include tokens, passwords, or secrets.
- [ ] `pre-commit run --all-files` passes.

## Before Pull Request

- [ ] The change supports MVP stabilization and is limited to the smallest practical scope.
- [ ] No unrelated refactoring is included.
- [ ] Controllers validate input, call services, and return DTOs.
- [ ] Business rules and transactions stay in the service layer.
- [ ] Entities, DTOs, and security principals are not mixed.
- [ ] Each microservice owns its own data.
- [ ] JWT validation, expiration, refresh flow, cookies, and filter order were reviewed if authentication changed.
- [ ] Maven tests pass for affected services:
  - [ ] `mvn -B -f backend/user-service/pom.xml test`
  - [ ] `mvn -B -f backend/task-service/pom.xml test`
  - [ ] `mvn -B -f infrastructure/api-gateway/pom.xml test`
  - [ ] `mvn -B -f infrastructure/config-server/pom.xml test`
  - [ ] `mvn -B -f infrastructure/eureka-server/pom.xml test`
- [ ] `./scripts/check-local-stack.sh` passes when startup behavior is affected.

## Before Merge

- [ ] README or `doc/` was updated for architecture, API contract, startup, or workflow changes.
- [ ] New technical debt was added to `doc/technical-debt.md` if it cannot be addressed in the current PR.
- [ ] No business logic, API contract, framework version, or runtime dependency changes are included unless they are the stated purpose of the PR.
