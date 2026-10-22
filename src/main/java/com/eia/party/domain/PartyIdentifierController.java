package com.eia.party.domain;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    /**
     * Applies an update to one identifier. The tenant travels on both the expert query string
     * and the {@code X-EIA-Tenant} header, and the command body is forwarded unchanged so the
     * expert remains the owner of validation, normalization, and duplicate detection. The expert
     * projection of the updated identifier is returned.
     */
    @PatchMapping("/{identifierId}")
    public PartyIdentifierDto update(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID partyId,
            @PathVariable UUID identifierId,
            @RequestBody ObjectNode body) {
        return client.updateIdentifier(tenantId, partyId, identifierId, body);
    }

    /**
     * Deletes one identifier through the expert. An unknown or foreign identifier is the
     * expert's {@code 404} and a refused delete keeps its {@code 409}; a successful delete is
     * {@code 204 No Content}.
     */
    @DeleteMapping("/{identifierId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID partyId,
            @PathVariable UUID identifierId) {
        client.deleteIdentifier(tenantId, partyId, identifierId);
    }
}
