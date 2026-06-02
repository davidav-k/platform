# Redis

Redis 7 runs as part of the local Docker Compose stack.

## Current Status

- Published on `localhost:6379`.
- Health-checked with `redis-cli ping`.
- Not currently integrated into `user-service`.
- Not used to store stateless JWT access or refresh tokens.

Verify Redis through the full local startup check:

```bash
./scripts/check-local-stack.sh
```
