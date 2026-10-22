package com.eia.party.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requested party lifecycle transition. The Party expert owns the status
 * vocabulary and transition rules, so the domain validates only the request
 * shape and forwards the status and reason unchanged.
 */
public record UpdatePartyStatusCommand(
        @NotBlank String status,
        @Size(max = 500) String reason
) {}
