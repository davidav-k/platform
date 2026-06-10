# Copilot Instructions

Project: Platform

Follow AGENTS.md first.

Additional rules:

* Keep changes small and atomic.
* One branch = one concern.
* Preserve existing API contracts unless explicitly requested.
* Do not introduce new dependencies without justification.
* Do not change Spring Boot, Spring Cloud, Java, PostgreSQL, Redis, or Docker versions without compatibility verification.
* Do not generate placeholder implementations that are not used.
* Do not add abstractions for future requirements.

Always:

1. Analyze before coding.
2. Explain planned changes.
3. Modify only necessary files.
4. Keep controllers thin.
5. Keep business logic in services.
6. Preserve DTO boundaries.
7. Verify security impact.
8. Suggest tests.

When finished provide:

## Summary

## Modified Files

## Validation

## Recommended Tests

## Documentation Changes

If information is missing:

* stop
* explain assumptions
* request clarification instead of guessing
