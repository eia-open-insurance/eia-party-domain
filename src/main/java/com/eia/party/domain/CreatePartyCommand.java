package com.eia.party.domain;

import jakarta.validation.constraints.NotBlank;

public record CreatePartyCommand(
        @NotBlank String partyType,
        @NotBlank String displayName,
        @NotBlank String legalName
) {}
