# Notification Service

## Status

Planned service. This directory currently contains documentation only. There
is no Maven module, application code, database schema, or Docker Compose
container for `notification-service`.

## Future Responsibility

`notification-service` will own platform notifications, notification
preferences, email delivery requests, templates, and delivery tracking.
Existing account-verification email remains in `user-service` during MVP
stabilization.

The future HTTP API is defined in
[Notification service API contract](../../doc/api/notification-service-contract.md).
Aggregate ownership is defined in
[Service boundaries](../../doc/architecture/service-boundaries.md).

Kafka, push delivery, and WebSocket delivery are future directions, not current
dependencies.
