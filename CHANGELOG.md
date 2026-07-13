# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `@Idempotent` annotation to make `POST`/`PATCH` endpoints idempotent via the `Idempotency-Key` header.
- `IdempotencyStore` SPI with a thread-safe in-memory implementation (per-key atomic reservations, TTL).
- HTTP handling: replay of the stored response, `409 Conflict` for in-progress keys, `422 Unprocessable
  Entity` for a key reused with a different request.
- Spring Boot auto-configuration with configuration properties (`idempotency.*`) and conditional beans.
- Runnable `sample-app` demonstrating the behaviour.

[Unreleased]: https://github.com/arthurfaby/spring-boot-starter-idempotency/commits/main
