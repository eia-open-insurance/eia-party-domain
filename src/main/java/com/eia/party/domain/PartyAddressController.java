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
 * Party addresses. The Party expert owns address-type validation, country normalization, and
 * tenant scoping; this gateway forwards {@code X-EIA-Tenant} and the caller's command body and
 * preserves expert {@code 400}/{@code 404}/{@code 409} failures.
 */
@RestController
@RequestMapping("/api/v1/parties/{partyId}/addresses")
public class PartyAddressController {
    private final PartyClient client;

    public PartyAddressController(PartyClient client) {
        this.client = client;
    }

    @GetMapping
    public List<PartyAddressDto> list(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID partyId) {
        return client.findAddresses(tenantId, partyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartyAddressDto create(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID partyId,
            @RequestBody ObjectNode body) {
        return client.createAddress(tenantId, partyId, body);
    }

    /**
     * Applies an update to one address. The tenant travels on both the expert query string and
     * the {@code X-EIA-Tenant} header, and the command body is forwarded unchanged so the expert
     * remains the owner of validation and normalization. The expert projection of the updated
     * address is returned.
     */
    @PatchMapping("/{addressId}")
    public PartyAddressDto update(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID partyId,
            @PathVariable UUID addressId,
            @RequestBody ObjectNode body) {
        return client.updateAddress(tenantId, partyId, addressId, body);
    }

    /**
     * Deletes one address through the expert. An unknown or foreign address is the expert's
     * {@code 404} and a refused delete keeps its {@code 409}; a successful delete is
     * {@code 204 No Content}.
     */
    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID partyId,
            @PathVariable UUID addressId) {
        client.deleteAddress(tenantId, partyId, addressId);
    }
}
