package com.eia.party.domain;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Common global-search endpoint for the Party vertical. It is separate from
 * {@link PartyController} so the existing list/get/create routes and their
 * expert proxy behavior stay unchanged.
 */
@RestController
@RequestMapping("/api/v1/parties/search")
public class PartySearchController {
    private final PartySearchService service;

    public PartySearchController(PartySearchService service) {
        this.service = service;
    }

    @GetMapping
    public SearchResponse search(@RequestHeader("X-EIA-Tenant") UUID tenantId,
                                 @RequestParam(defaultValue = "") String q,
                                 @RequestParam(required = false) Integer limit) {
        return service.search(tenantId, q, limit);
    }
}
