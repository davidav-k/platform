# Documentation Audit

## Scope

This audit aligns developer-facing documentation with the current MVP
implementation. It does not change application code, configuration, public API
contracts, or infrastructure.

## Documentation Reviewed

- Root [README](../../readme.md)
- [Architecture overview](../architecture.md)
- [Service boundaries](../architecture/service-boundaries.md)
- API contracts under `doc/api/`
- [Environment variables](../configuration/env-variables.md)
- [Database migration strategy](../database/migration-strategy.md)
- [Authentication flow](../security/auth-flow.md)
- [Health checks](../operations/health-checks.md)
- Development workflow, checklist, and technical-debt documents
- Service and infrastructure READMEs under `backend/`, `frontend/`, and
  `infrastructure/`
- `compose.yml`, `.env.example`, Spring configuration files, Dockerfiles,
  Maven POM files, Flyway migration files, and `.github/workflows/maven-tests.yml`

## Verified Implementation State

### Implemented Java Modules

| Module | Version baseline | Responsibility |
| --- | --- | --- |
| `backend/user-service` | Java 17, Spring Boot 3.4.0, Spring Cloud 2024.0.1 | Users, authentication, authorization, profiles, account lifecycle, MFA, and Flyway migrations |
| `infrastructure/config-server` | Java 17, Spring Boot 3.4.0, Spring Cloud 2024.0.1 | Native configuration repository |
| `infrastructure/eureka-server` | Java 17, Spring Boot 3.4.0, Spring Cloud 2024.0.1 | Service discovery |
| `infrastructure/api-gateway` | Java 17, Spring Boot 3.4.0, Spring Cloud 2024.0.1 | User-route gateway, JWT early rejection, CORS, and circuit breaker fallback |

### Docker Compose Stack

| Service | Image or module | Published port |
| --- | --- | --- |
| `postgres` | `postgres:16.1` | `5432` |
| `redis` | `redis:7` | `6379` |
| `zipkin` | `openzipkin/zipkin` | `9411` |
| `mailhog` | `mailhog/mailhog` | `1025`, `8025` |
| `config-server` | Local Java module | `8888` |
| `eureka-server` | Local Java module | `8761` |
| `user-service` | Local Java module | `8085` |
| `gateway` | Local Java module | `8080` |

### Skeleton Or Planned Components

- `backend/task-service` contains documentation only.
- `backend/notification-service` contains documentation only.
- `frontend/vue-frontend` contains documentation only.
- `infrastructure/kuberneres` contains documentation only.
- Kafka, audit service, OpenAI automation, Helm, Prometheus, and Grafana are
  roadmap items, not active runtime components.

## Inconsistencies Found

| Area | Previous documentation issue | Verified reality |
| --- | --- | --- |
| Architecture | Presented the full roadmap as implemented | Only user-service and supporting infrastructure are runnable |
| Database | Root README listed PostgreSQL 14 | Compose uses PostgreSQL 16.1 |
| Framework versions | Root README listed generic Spring Boot 3.x | All Java POMs use Spring Boot 3.4.0 and Spring Cloud 2024.0.1 |
| Redis | READMEs implied Redis stores access tokens or is the user-service cache | JWTs are stateless; user-service uses an in-process Guava cache |
| Frontend | Frontend README claimed a working Vue application | Directory contains only a README |
| Task service | Service README claimed implemented task management | Directory contains only a README |
| Notification service | Service README claimed active delivery and Kafka integration | Directory contains only a README |
| Kubernetes | README claimed manifests and Helm charts exist | Directory contains only a README |
| Gateway | README listed task routing without qualification | User route works; task route is reserved but no task-service module runs |
| Onboarding | Root README omitted explicit `.env.example` copy command | Local setup requires `.env` created from the development-only template |
| Startup verification | Workflow and checklist omitted the stack verification script | `scripts/check-local-stack.sh` is the repeatable startup check |
| Migration commands | Migration examples did not consistently use the root Compose invocation | Startup uses `--env-file .env -f compose.yml` |
| Component docs | Config Server and Eureka READMEs were empty headings | Both are active local infrastructure modules |

## Corrections Applied

- Rewrote the architecture overview around implemented, partially configured,
  and planned components.
- Added a visible `Current MVP Status` section to the root README.
- Corrected PostgreSQL, Spring Boot, and Spring Cloud versions.
- Clarified Redis, Zipkin, MailHog, Config Server, Eureka, and Gateway roles.
- Marked task-service, notification-service, frontend, and Kubernetes
  directories as planned placeholders.
- Added explicit local setup, startup, verification, and documentation links.
- Updated workflow and checklist documentation to include
  `./scripts/check-local-stack.sh`.
- Normalized Flyway local-verification commands.
- Expanded Config Server, Eureka, Docker, Redis, Gateway, and user-service
  component READMEs.
- Marked documentation normalization debt `TD-004` as resolved.

## Remaining Documentation Debt

- Educational notes under `doc/docSpring/` are reference material and are not
  maintained as platform implementation documentation. They should be moved
  under an explicitly labeled learning-notes directory or reviewed separately.
- The `infrastructure/kuberneres` directory name is misspelled. Renaming it is
  deferred until Kubernetes resources exist.
- Zipkin is present in Compose, but application tracing behavior is not
  documented as complete.
- IDE startup outside Docker still requires manual host adjustments because
  Config Server and Eureka client URLs use Docker-network hostnames.
- Gateway task routing currently uses `/api/v1/task/**`, while the future API
  contract uses `/api/v1/tasks/**`.

## Runtime Debt Discovered During Documentation Verification

These are implementation issues recorded in
[Technical debt](../technical-debt.md), not documentation changes:

- `UserResource.enableMfa` references `#userId` in a method-security expression,
  but the method has no `userId` parameter.
- Gateway error responses are not aligned with the user-service response
  envelope.

## Recommended Future Documentation Work

- Add task-service and notification-service runbooks when their Maven modules
  and containers exist.
- Add frontend setup instructions only after a package manifest and source tree
  are committed.
- Add event schema and delivery-guarantee documentation before introducing a
  broker.
- Add Kubernetes deployment instructions only with tested manifests or Helm
  charts.
- Add observability documentation when tracing and metrics integrations are
  implemented.
- Re-run this audit whenever the Compose stack, environment contract, routing,
  or module inventory changes.
