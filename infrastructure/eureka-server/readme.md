# Eureka Server

Netflix Eureka service-discovery server for the local MVP.

## Current Behavior

- Runs on `http://localhost:8761`.
- Loads configuration through Config Server.
- Does not register itself with Eureka or fetch its own registry.
- Exposes `/actuator/health` and `/actuator/info`.

Verify registry access:

```bash
curl -fsS http://localhost:8761/eureka/apps
```

See [Health checks](../../doc/operations/health-checks.md).
