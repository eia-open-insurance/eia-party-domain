package com.eia.party.domain;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Party identifiers. The Party expert owns identifier-type validation, duplicate detection, and
 * tenant scoping; this gateway forwards {@code X-EIA-Tenant} and the caller's command body and
 * preserves expert {@code 400}/{@code 404}/{@code 409} failures.
 */
@RestController
@RequestMapping("/api/v1/parties/{partyId}/identifiers")
public class PartyIdentifierController {
    private final PartyClient client;

    public PartyIdentifierController(PartyClient client) {
        this.client = client;
    }

    @GetMapping
    public List<PartyIdentifierDto> list(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID partyId) {
        return client.findIdentifiers(tenantId, partyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartyIdentifierDto create(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID partyId,
            @RequestBody ObjectNode body) {
        return client.createIdentifier(tenantId, partyId, body);
    }
}
