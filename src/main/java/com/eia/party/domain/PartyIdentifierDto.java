package com.eia.party.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Public projection of one party identifier (SPEC 11.1.4 {@code party_identifier}).
 *
 * <p>The Party expert owns identifier-type validation, duplicate detection, and tenant scoping.
 * The domain does not duplicate those rules: it forwards the caller's command and returns this
 * projection of the expert's record.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PartyIdentifierDto(
        UUID id,
        UUID tenantId,
        UUID partyId,
        String identifierType,
        String identifierValue,
        String issuingAuthority,
        LocalDate effectiveDate,
        Instant createdAt,
        String createdBy
) {
}
