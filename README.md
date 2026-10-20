# eia-party-domain

- GitHub organization: `eia-open-insurance`
- Repository URL: https://github.com/eia-open-insurance/eia-party-domain
- Type: Spring Boot service
- Deployable: Yes
- Owner: Party
- Runtime: Java 22 and Spring Boot
- Data store: eia_party (tx schema)
- Depends on: eia-party-expert, eia-contracts, eia-platform/libs

## Purpose

Party business rules, transaction catalog, orchestration, approvals, and APIs. Party identity, roles, merge, KYC/AML, sanctions, and consent.

## Global search

`GET /api/v1/parties/search?q={term}&limit=10` returns the common shell search
envelope `{query, limit, items}`. `q` is a case-insensitive contains over
`partyNumber`, `displayName`, and `legalName`; a blank `q` returns the latest
records by `updatedAt DESC`. `limit` defaults to 10 and is capped at 25. Every
query is scoped to `X-EIA-Tenant`. The read uses `JdbcTemplate` against the
`eia_party` data store; the existing list/get/create routes keep proxying the
Party expert unchanged.

## Responsibilities

- Keep this repository focused on one deployable component or one platform concern.
- Follow the repository standard in `../CONVENTIONS.md` and the specification in `../../SPEC.md`.
- Publish and consume contracts through `eia-contracts`.
- Maintain README, CODEOWNERS, SECURITY, CHANGELOG, tests, and local verification.
- Contain no business logic in shared libraries or front-end remotes.

## Verification

- Lint and format
- Unit tests with 100% business-logic coverage where applicable
- Contract tests
- Integration or Playwright tests where applicable
- Security scan and SBOM
- Build and publish

## Notes

Owns the business logic for this vertical.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
