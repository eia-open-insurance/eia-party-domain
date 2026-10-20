package com.eia.party.domain;

import java.util.UUID;

/**
 * Projection read by the Party search query; mapping to the public envelope
 * happens in {@link PartySearchService}.
 */
record PartySearchRow(
        UUID id,
        String partyNumber,
        String displayName,
        String partyType,
        String status
) {}
