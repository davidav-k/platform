# Configuration Management

## Configuration Ownership

Config Server is the primary source of truth for service runtime
configuration. For `user-service`, the active development runtime settings are
served from `config/user-service-dev.yml`.

Configuration precedence for local development is:

1. Environment variables for secrets and environment-specific overrides.
2. Config Server files under `config/` for service runtime settings.
3. Bundled local resources for Config Server bootstrap metadata and safe
   developer convenience defaults only.

Runtime configuration must not be duplicated in a service's bundled
`application.yml`. Add datasource, JPA, Flyway, JWT, cookie, mail, Redis,
Eureka, logging, feature flags, and service-specific behavior settings to the
appropriate Config Server file. Reference environment variables rather than
committing secrets.

The user-service bundled resources intentionally contain:

- `application.yml`: `spring.application.name`.
- `bootstrap.yml`: the default `dev` profile, Config Server client settings,
  and an overridable `CONFIG_SERVER_URI`.
- `application-dev.yml`: a marker for the retained local development profile.

## Docker Compose Startup

From the repository root:

```bash
cp .env.example .env
docker compose --env-file .env -f compose.yml up -d --build
./scripts/check-local-stack.sh
```

Docker uses the tracked network defaults:

- Config Server: `http://config-server:8888`
- Eureka Server: `http://eureka-server:8761/eureka`
- PostgreSQL host: `postgres`
- MailHog SMTP host: `mailhog`

Compose mounts `./config` into Config Server at `/config`. Services load their
runtime settings from Config Server after it becomes healthy.

## IntelliJ User-Service Startup

Start PostgreSQL, MailHog, Config Server, and Eureka with Docker Compose. Run
`user-service` from IntelliJ with the `dev` profile and the same values as the
root `.env`, except for these host overrides:

```text
CONFIG_SERVER_URI=http://localhost:8888
EUREKA_URL=http://localhost:8761/eureka
POSTGRES_HOST=localhost
EMAIL_HOST=localhost
```

No tracked YAML edits are required. Config Server still remains authoritative
for runtime settings during an IDE launch.

## Adding Properties

When adding a new user-service runtime property:

1. Add it to `config/user-service-dev.yml`.
2. Use an environment-variable placeholder when the value is secret or differs
   by environment.
3. Add the variable to `.env.example` and
   `doc/configuration/env-variables.md` when developers must provide it.
4. Do not add the runtime property to bundled `application.yml`.

Create profile-specific Config Server files when additional deployed
environments are introduced. Do not commit production secrets.
