package com.eia.party.domain;

import java.util.UUID;

/**
 * One row of the common global-search result shape. Party emits {@code type}
 * {@code PARTY}; the shell merges this with the other verticals.
 */
public record SearchItem(
        UUID id,
        String type,
        String label,
        String secondary,
        String status,
        String href
) {}
