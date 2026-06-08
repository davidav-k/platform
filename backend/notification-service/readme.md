# Notification Service

## Status

Bootstrap module created. The service is expected to use port `8087` in a
future deployment, but it is not wired into Docker Compose yet.

## Configuration and Database

Runtime configuration is served by Config Server from
`config/notification-service-dev.yml`. Flyway owns schema migrations, and the
database name defaults to `notifications_db`.

## Not Implemented Yet

Notification domain logic, REST API, Gateway routing, Docker Compose service,
Kafka integration, and email delivery remain for later branches.
