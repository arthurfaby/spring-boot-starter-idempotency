# Contributing

Thanks for your interest in improving `spring-boot-starter-idempotency`! 🙌

## Getting started

```bash
git clone https://github.com/arthurfaby/spring-boot-starter-idempotency.git
cd spring-boot-starter-idempotency
./mvnw verify
```

No local Maven install is required — the project ships the Maven Wrapper (`./mvnw`).

## Development workflow

1. Create a branch: `git checkout -b feat/short-description`.
2. Make your change **with tests**.
3. Format the code: `./mvnw spotless:apply` (the build fails on unformatted code).
4. Run the full build: `./mvnw verify`.
5. Open a pull request.

## Code style

- Formatting is enforced by [Spotless](https://github.com/diffplug/spotless) using
  [Palantir Java Format](https://github.com/palantir/palantir-java-format). Run `./mvnw spotless:apply`
  before committing.
- Public API (annotations, SPI, configuration properties) must be documented with Javadoc.

## Commit messages

This project uses [Conventional Commits](https://www.conventionalcommits.org/):
`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `build:`, `ci:`, `chore:`.

## Tests

- Unit tests with JUnit 5 + AssertJ.
- Integration tests with `@SpringBootTest` + MockMvc.
- New behaviour must come with a test; concurrency-sensitive code must be covered by a concurrency test.

## Reporting bugs / requesting features

Use the [issue templates](.github/ISSUE_TEMPLATE). For security issues, see [SECURITY.md](SECURITY.md).

## License

By contributing, you agree that your contributions are licensed under the
[Apache License 2.0](LICENSE).
