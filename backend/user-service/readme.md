# User Service

Implemented service for:

- User registration
- Authentication using JWT
- Role management
- Profiles and account lifecycle
- MFA
- Flyway-managed PostgreSQL persistence

## Technologies
- Java 17
- Spring Boot 3.4.0
- Spring Cloud 2024.0.1
- PostgreSQL for user data storage
- Flyway for schema migrations
- Google Guava for caching
- Spring Security for authentication and authorization
- Spring Mail for email notifications
- Docker for containerization
- JUnit and Mockito for testing
- Lombok for reducing boilerplate code

## Endpoints and Permissions

These are internal service paths. API Gateway exposes user traffic under
`/api/users/**` and rewrites it to `/api/v1/user/**`.

### User Management
- **POST** `/api/v1/user/register` - Register a new user. **Requires:** No authentication
- **GET** `/api/v1/user/verify/account` - Verify a new user account using a key. **Requires:** No authentication
- **POST** `/api/v1/user/login` - Log in a user. **Requires:** No authentication
- **POST** `/api/v1/user/enable-mfa` - Enable MFA for the authenticated user's own account. **Requires:** Valid access token
- **POST** `/api/v1/user/verify-mfa` - Verify MFA for a user. **Requires:** No authentication
- **POST** `/api/v1/user/unlock` - Unlock a user account. **Requires:** `user:update`
- **POST** `/api/v1/user/lock` - Lock a user account. **Requires:** `user:update`
- **POST** `/api/v1/user/refresh` - Refresh access and refresh tokens. **Requires:** Valid refresh token
- **GET** `/api/v1/user/profile` - Retrieve the profile of the logged-in user. **Requires:** Valid access token
- **PUT** `/api/v1/user/{userId}` - Update user details. **Requires:** `user:update` or be the owner of the account
- **PATCH** `/api/v1/user/password/{userId}` - Change a user's password. **Requires:** `user:update` or be the owner of the account
- **DELETE** `/api/v1/user/{userId}` - Hard-delete a user and user-service-owned dependent data. **Requires:** `user:delete`. **Restrictions:** system user `0` and self-deletion are rejected

API Gateway exposes password changes externally as
`PATCH /api/users/password/{userId}`.

For the full registration, login, refresh, JWT, and cookie behavior, see
[Authentication flow](../../doc/security/auth-flow.md).

### Local Launch

Run from the repository root:

```bash
cp .env.example .env
docker compose --env-file .env -f compose.yml up -d --build
./scripts/check-local-stack.sh
```

MailHog UI is available at `http://localhost:8025`.

See [Environment variables](../../doc/configuration/env-variables.md) and
[Database migration strategy](../../doc/database/migration-strategy.md).

### Configuration Ownership

Config Server is the primary source of truth for runtime configuration.
For an IntelliJ launch, override the following environment variables:

```text
CONFIG_SERVER_URI=http://localhost:8888
EUREKA_URL=http://localhost:8761/eureka
POSTGRES_HOST=localhost
EMAIL_HOST=localhost
```

See [Configuration management](../../doc/architecture/configuration-management.md)
for the ownership hierarchy and startup modes.
