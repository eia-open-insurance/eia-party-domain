# Changelog

All notable changes to this repository are documented here.

The format is based on Keep a Changelog and the project uses Semantic Versioning.

## [Unreleased]

### Added

- Repository baseline.
- `GET /api/v1/parties/search` with the shared global-search envelope
  `{query, limit, items}`, case-insensitive contains matching, tenant scoping,
  a 10-default / 25-capped limit, and a `/party?record=<id>` href.
- `GET` and `POST /api/v1/parties/{partyId}/identifiers` and
  `/api/v1/parties/{partyId}/addresses`, proxying the tenant and command body to
  the Party expert and returning typed `PartyIdentifierDto`/`PartyAddressDto`
  projections. Expert `400`, `404`, and `409` responses keep their status and
  structured error code.
