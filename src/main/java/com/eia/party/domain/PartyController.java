package com.eia.party.domain;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parties")
public class PartyController {
    private final PartyClient client;

    public PartyController(PartyClient client) {
        this.client = client;
    }

    @GetMapping
    public List<PartyDto> list(@RequestHeader("X-EIA-Tenant") UUID tenantId) {
        return client.findAll(tenantId);
    }

    @GetMapping("/{id}")
    public PartyDto get(@RequestHeader("X-EIA-Tenant") UUID tenantId, @PathVariable UUID id) {
        return client.findById(tenantId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartyDto create(@RequestHeader("X-EIA-Tenant") UUID tenantId,
                           @Valid @RequestBody CreatePartyCommand command) {
        return client.create(tenantId, command);
    }

    /**
     * Applies one party status transition through the expert. The tenant travels
     * on both the query string and the {@code X-EIA-Tenant} header, and the
     * optional {@code If-Match} and {@code Idempotency-Key} headers are forwarded
     * unchanged. The expert owns the status vocabulary and the transition rules;
     * its {@code 400}/{@code 404} problems keep their status and structured error.
     * The response carries the updated party with an {@code ETag} built from its
     * aggregate version.
     */
    @PatchMapping("/{partyId}/status")
    public ResponseEntity<PartyDto> updateStatus(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID partyId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody UpdatePartyStatusCommand command) {
        PartyDto party = client.updateStatus(tenantId, partyId, command, ifMatch, idempotencyKey);
        return ResponseEntity.ok()
                .eTag(Integer.toString(party.version()))
                .body(party);
    }
}
