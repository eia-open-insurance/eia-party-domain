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

## Identifiers and addresses

`GET`, `POST`, `PATCH`, and `DELETE /api/v1/parties/{partyId}/identifiers` and
`/api/v1/parties/{partyId}/addresses` expose the party's identifier and address
records; `PATCH` and `DELETE` address one record through
`/{identifierId}` and `/{addressId}`. Every route requires `X-EIA-Tenant`; the
domain forwards that tenant on the query string and the header to the Party
expert's internal route and forwards the write body unchanged. `PATCH` sends
the replacement values for the record's mutable fields and returns the expert
projection of the updated record. `DELETE` returns `204 No Content`. The expert
owns type validation, country normalization, duplicate detection, and the
tenant wall.

- Identifier projection: `id`, `tenantId`, `partyId`, `identifierType`,
  `identifierValue`, `issuingAuthority`, `effectiveDate`, `createdAt`,
  `createdBy`.
- Address projection: `id`, `tenantId`, `partyId`, `addressType`, `line1`,
  `line2`, `city`, `stateRegion`, `postalCode`, `country`, `effectiveDate`,
  `createdAt`, `createdBy`.
- Expert `400`, `404`, and `409` responses keep their status and structured
  `code`/`message`; the domain does not collapse a duplicate into a validation
  error or a missing party into a server error.

```http
POST /api/v1/parties/0192f3a4-8f1c-7c3a-9b2e-6a1d4f8e2b90/identifiers
X-EIA-Tenant: 11111111-1111-7111-8111-111111111111
Content-Type: application/json

{"identifierType":"TAX_ID","identifierValue":"123-45-6789","issuingAuthority":"IRS"}
```

```http
PATCH /api/v1/parties/0192f3a4-8f1c-7c3a-9b2e-6a1d4f8e2b90/identifiers/0192f3a4-9000-7c3a-9b2e-6a1d4f8e2b90
X-EIA-Tenant: 11111111-1111-7111-8111-111111111111
Content-Type: application/json

{
  "identifierType": "TAX_ID",
  "identifierValue": "123-45-6789",
  "issuingAuthority": "Internal Revenue Service",
  "effectiveDate": "2026-01-20"
}
```

`PATCH` returns `200 OK` with the updated identifier or address projection.
`DELETE` returns `204 No Content`. A missing record is `404`, a duplicate
identifier is `409`, and a rejected body is `400`, each with the expert's
structured `code` and `message`.

## Status transitions

`PATCH /api/v1/parties/{partyId}/status` applies one party lifecycle transition
through the Party expert with the `{status, reason}` command. The route requires
`X-EIA-Tenant` and forwards it on the expert query string and the
`X-EIA-Tenant` header; the optional `If-Match` and `Idempotency-Key` headers are
forwarded unchanged. The response is the updated party DTO with an `ETag` built
from its aggregate `version`. The expert owns the status vocabulary and the
transition rules, and its `400`/`404` responses keep their status and structured
`code`/`message`.

```http
PATCH /api/v1/parties/0192f3a4-8f1c-7c3a-9b2e-6a1d4f8e2b90/status
X-EIA-Tenant: 11111111-1111-7111-8111-111111111111
If-Match: "4"
Idempotency-Key: 0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72
Content-Type: application/json

{"status":"ACTIVE","reason":"KYC verified"}
```

A successful transition returns `200 OK`, the updated party body, and
`ETag: "5"`.

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
