package com.eia.party.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Public projection of one party address (SPEC 11.1.4 {@code party_address}).
 *
 * <p>The Party expert owns address-type validation, country normalization, and tenant scoping.
 * The domain does not duplicate those rules: it forwards the caller's command and returns this
 * projection of the expert's record.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PartyAddressDto(
        UUID id,
        UUID tenantId,
        UUID partyId,
        String addressType,
        String line1,
        String line2,
        String city,
        String stateRegion,
        String postalCode,
        String country,
        LocalDate effectiveDate,
        Instant createdAt,
        String createdBy
) {
}
