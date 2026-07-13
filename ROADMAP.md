# Roadmap

Milestones follow [Semantic Versioning](https://semver.org/). The MVP is usable in production at small
scale; larger deployments are covered by the persistent stores below.

## v0.1.0 — MVP (current)

- `@Idempotent` annotation + `Idempotency-Key` header handling.
- Thread-safe in-memory store with TTL.
- Response replay, `409` (in progress), `422` (different request).
- Auto-configuration + configuration properties.

## v0.2.0 — JDBC store

- `JdbcIdempotencyStore` for multi-instance persistence (Flyway migration provided).
- Tested against PostgreSQL with Testcontainers.

## v0.3.0 — Redis store

- `RedisIdempotencyStore` with a distributed lock for cross-instance concurrency.

## v0.4.0 — Observability

- Micrometer metrics (`idempotency.hits`, `.misses`, `.conflicts`) and an Actuator endpoint.

## v1.0.0 — Stable API

- Frozen public API and complete documentation.
- Full request fingerprint (method + path + body hash).
- Purge policy for expired keys.
