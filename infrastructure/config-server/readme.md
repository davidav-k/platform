# Config Server

Spring Cloud Config Server for the local MVP.

## Current Behavior

- Runs on `http://localhost:8888`.
- Uses the native profile.
- Serves configuration mounted from the root `./config` directory.
- Exposes `/actuator/health` and `/actuator/info`.

Verify repository access:

```bash
curl -fsS http://localhost:8888/user-service/dev
```

See [Environment variables](../../doc/configuration/env-variables.md) and
[Health checks](../../doc/operations/health-checks.md).
