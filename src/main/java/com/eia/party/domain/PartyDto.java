package com.eia.party.domain;

import java.time.Instant;
import java.util.UUID;

public record PartyDto(
        UUID id,
        UUID tenantId,
        String partyNumber,
        String partyType,
        String displayName,
        String legalName,
        String status,
        String sanctionsStatus,
        String confidentiality,
        int version,
        Instant createdAt,
        Instant updatedAt
) {}
