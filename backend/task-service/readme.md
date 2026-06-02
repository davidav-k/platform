# Task Service

## Status

Planned service. This directory currently contains documentation only. There
is no Maven module, application code, database schema, or Docker Compose
container for `task-service`.

## Future Responsibility

`task-service` will own tasks, assignments, statuses, comments, and task
history. It must not access the `user-service` database directly.

The future HTTP API is defined in
[Task service API contract](../../doc/api/task-service-contract.md). Aggregate
ownership and gateway alignment notes are defined in
[Service boundaries](../../doc/architecture/service-boundaries.md).

Kafka integration is a future direction, not a current dependency.
