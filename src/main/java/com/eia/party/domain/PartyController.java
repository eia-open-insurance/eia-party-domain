package com.eia.party.domain;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
}
