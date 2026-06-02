# Authentication Flow

## Authentication Overview

The MVP uses stateless HMAC-signed JWTs issued by `user-service`. The browser
receives two `HttpOnly` cookies:

| Cookie | Purpose | Default browser lifetime |
| --- | --- | --- |
| `access-token` | Authenticates protected requests | 600 seconds |
| `refresh-token` | Rotates both cookies through the refresh endpoint | 7200 seconds |

Both JWT types use `jwt.expiration-seconds`, backed by `JWT_EXPIRATION`. The
default local value is `432000` seconds (5 days). Browser cookie expiry is
intentionally shorter than the signed JWT expiry.

The API Gateway accepts `access-token` or `Authorization: Bearer <token>` for
protected gateway routes. Cookies and authorization headers are forwarded to
downstream services. `user-service` independently validates the access JWT and
owns authorization decisions.

## Registration Flow

External endpoint: `POST /api/users/register`

Internal endpoint after gateway rewrite: `POST /api/v1/user/register`

Request DTO: `UserRequest`

Sequence:

1. Gateway allows the public registration path and rewrites it.
2. `UserResource.saveUser` validates `UserRequest` and calls the service.
3. `UserServiceImpl.createUser` rejects an existing email.
4. The service uses system actor `0` for anonymous registration auditing,
   creates a disabled user with the `USER` role, and stores the encoded
   password.
5. The service stores a confirmation key and publishes a registration event.
6. The email listener sends the account-verification link.
7. `GET /api/users/verify/account?key=...` is public. `user-service` validates
   the key, enables the account, and deletes the confirmation record.

## Login Flow

External endpoint: `POST /api/users/login`

Internal endpoint after gateway rewrite: `POST /api/v1/user/login`

Request DTO: `LoginRequest`

Sequence:

1. Gateway allows the public login path and rewrites it.
2. `AuthenticationFilter` reads and validates the JSON credentials request.
3. The filter records the login attempt and delegates to Spring's
   `AuthenticationManager`.
4. `ApiAuthenticationProvider` loads the user and credential, checks account
   state, and verifies the encoded password.
5. On success, the filter records login success.
6. When MFA is disabled, the filter issues both cookies and returns the user
   DTO. When MFA is enabled, it returns the MFA prompt without cookies.

Password login is intentionally handled by the filter, not by a controller.

## MFA Enrollment Flow

External endpoint: `POST /api/users/enable-mfa`

Internal endpoint after gateway rewrite: `POST /api/v1/user/enable-mfa`

Sequence:

1. Gateway and `user-service` require a valid access JWT.
2. `UserResource.enableMfa` reads the authenticated `User` principal.
3. The controller calls `UserService.enableMfa` with the principal's email.
4. The service generates and stores the MFA secret and QR-code URL for that
   account.

Enrollment is self-service only. The endpoint does not accept an email query
parameter and cannot target another account. MFA secrets and OTP codes must
not be logged.

## Refresh Flow

External endpoint: `POST /api/users/refresh`

Internal endpoint after gateway rewrite: `POST /api/v1/user/refresh`

Sequence:

1. Gateway allows refresh without an access token so an expired access session
   can recover.
2. `UserResource.refreshTokens` requires the `refresh-token` cookie.
3. `JwtServiceImpl` verifies the JWT signature and registered time claims, then
   loads the subject user.
4. The controller issues new access and refresh cookies.

The refresh cookie is rotated, but there is no server-side token store,
revocation list, or one-time-use enforcement.

## Logout Flow

There is currently no public logout endpoint. `JwtService.removeCookie`
supports explicit cookie deletion, but it is not exposed by a controller.
Because tokens are stateless, cookie deletion would not invalidate a copied
JWT. Server-side revocation remains technical debt.

## JWT Validation Flow

Protected request sequence:

1. Client calls a gateway route with `access-token` or a bearer header.
2. Gateway `AuthenticationFilter` verifies the JWT signature and time claims.
3. Gateway adds `X-Authenticated-User` for downstream informational use and
   forwards the original cookie or authorization header.
4. `user-service` `JwtAuthenticationFilter` extracts the access cookie first,
   then falls back to the bearer header.
5. `JwtServiceImpl` verifies the JWT again, loads the subject user, and derives
   authorities from access-token claims.
6. The filter populates Spring `SecurityContext` and `RequestContext`.
7. Controllers use the Spring principal; method security enforces authority
   checks.
8. `RequestContext` is cleared in `finally`. Login response handling and admin
   bootstrap also clear their temporary request context values. Spring Security owns
   `SecurityContext` cleanup through its filter chain.

Gateway validation is an early rejection layer. Downstream validation remains
required because authorization is owned by each service and internal traffic
must not trust `X-Authenticated-User` alone.

## Security Components

| Component | Responsibility |
| --- | --- |
| Controller | Validate request DTOs, call services, return DTO responses |
| Service layer | User creation, account confirmation, login audit, refresh user lookup |
| `JwtServiceImpl` | Create, parse, validate, extract, issue, and remove JWT cookies |
| Login `AuthenticationFilter` | Own password-login HTTP processing |
| `JwtAuthenticationFilter` | Authenticate protected user-service requests and manage `RequestContext` |
| User-service `SecurityConfig` | Define public paths, filter order, stateless sessions, and authorization |
| Gateway `AuthenticationFilter` | Reject missing or invalid access JWTs before protected routes |
| Gateway `SecurityConfig` | Permit routing so the gateway `GlobalFilter` can enforce its path policy |

## Cookie Strategy

Cookie behavior is explicit under `auth.cookie`:

| Property | Local default | Notes |
| --- | --- | --- |
| `secure` | `false` | Set `AUTH_COOKIE_SECURE=true` for HTTPS deployments |
| `same-site` | `Lax` | Set deliberately for the deployed frontend topology |
| `access-max-age-seconds` | `600` | Access-cookie browser lifetime |
| `refresh-max-age-seconds` | `7200` | Refresh-cookie browser lifetime |

Both cookies use `HttpOnly=true` and `Path=/`. Cookie values, JWT values,
refresh tokens, and account-confirmation keys must never be logged.

## Security Assumptions

- `JWT_SECRET` is Base64 encoded and shared by the Gateway and `user-service`.
- HTTPS deployments set `AUTH_COOKIE_SECURE=true`.
- The browser sends credentials for cookie-authenticated cross-origin calls.
- Downstream services validate JWTs independently and do not trust gateway
  headers as the sole authentication proof.
- CSRF protection is disabled. `SameSite=Lax` reduces exposure but does not
  replace an explicit CSRF design for every deployment topology.

## Future Keycloak Migration Notes

A future Keycloak migration should replace local password authentication and
token issuance with an OAuth 2.0 / OpenID Connect flow. Keep downstream
authorization DTOs separate from security principals, validate issuer and
audience, and map external roles deliberately. Do not trust gateway headers as
the only downstream credential after migration.
