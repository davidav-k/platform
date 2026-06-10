# Development Workflow

This repository is being stabilized for the MVP. Keep changes small, explicit, and easy to review.

## Branching

- `main` is the stable integration branch.
- `dev` is the active development integration branch.
- Feature and fix branches should branch from the intended target branch and use short descriptive names, for example `fix/user-service-startup` or `docs/ci-workflow`.
- Open pull requests into `dev` during MVP development unless the change is an urgent repository maintenance fix for `main`.

### Expected Branch Protection

Branch protection is configured in GitHub and cannot be verified from
repository files. A repository administrator should configure and periodically
verify the following rules.

For `main`:

- Require a pull request before merge.
- Disable direct pushes.
- Require all checks produced by the `Maven Tests` workflow to pass.
- Require the branch to be up to date before merge.
- Require all review conversations to be resolved.
- Require at least one approving review.
- Enable repository-level secret scanning and push protection.

For `dev`:

- Require a pull request before merge.
- Require all checks produced by the `Maven Tests` workflow to pass.
- Disable direct pushes when practical; otherwise, direct pushes are strongly discouraged.
- Recommend at least one approving review.
- Recommend resolving all review conversations before merge.

These rules are expected repository settings, not claims about current GitHub
configuration. See [Repository Settings](repository-settings.md) for the
related secret-protection expectations.

## Pull Requests

Every pull request should:

- Use the repository pull request template.
- Target `dev` by default, or identify why `main` is the correct target.
- Describe the scope and validation performed.
- Keep unrelated refactoring out of MVP stabilization changes.
- Call out API contract, security, configuration, startup, or documentation impact.
- Include tests or a clear reason why tests are not applicable.
- Pass all required CI checks before merge.

## Local Setup

Create a local environment file from the example:

```bash
cp .env.example .env
```

Replace all placeholder values in `.env`. Do not commit `.env`.

Start the local stack from the repository root:

```bash
docker compose --env-file .env -f compose.yml up -d --build
./scripts/check-local-stack.sh
```

Stop the local stack:

```bash
docker compose --env-file .env -f compose.yml down
```

## Local Quality Gates

Install local pre-commit tooling:

```bash
pipx install pre-commit
pipx install ggshield
ggshield auth login
pre-commit install
```

Run all repository hooks manually:

```bash
pre-commit run --all-files
```

Run Maven tests for each Java service:

```bash
mvn -B -f backend/user-service/pom.xml test
mvn -B -f backend/task-service/pom.xml test
mvn -B -f backend/notification-service/pom.xml test
mvn -B -f infrastructure/api-gateway/pom.xml test
mvn -B -f infrastructure/config-server/pom.xml test
mvn -B -f infrastructure/eureka-server/pom.xml test
```

Docker Desktop or another compatible Docker daemon must be running for the
PostgreSQL integration tests. Most controller, security, and use-case tests
use Mockito or H2 and remain fast; repository, migration, and selected
integration tests use PostgreSQL 16.1 through Testcontainers.

All three backend services configure docker-java API version `1.44` through
Maven Surefire. Docker Engine 29 rejects the older API version used by the
current Testcontainers 1.x dependency. Do not override
`~/.testcontainers.properties` unless the local Docker transport requires it.

## Secret Handling

- Keep secrets in local `.env`, environment variables, or external secret managers.
- Do not commit `.env`, generated credentials, private keys, JWT tokens, passwords, or API keys.
- Treat values in `.env.example` as local placeholders only. Replace them before running anything outside local development.
- Run the configured `detect-private-key` and GitGuardian `ggshield` pre-commit hooks before pushing changes.
- Follow the GitHub secret scanning, push protection, and branch protection expectations in [Repository Settings](repository-settings.md).
- GitHub-level settings must be verified manually by a repository administrator; repository files do not prove that they are enabled.

## CI

GitHub Actions runs the modules listed in `.github/workflows/maven-tests.yml`
on pull requests and pushes to `main` and `dev`. The matrix runs tests for all
six buildable Maven modules: the three backend services, API gateway, Config
Server, and Eureka Server.

The repository does not currently have a root Maven aggregator. Add new Maven services to `.github/workflows/maven-tests.yml` when they become buildable modules.
