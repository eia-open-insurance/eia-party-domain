package com.eia.party.domain;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Maps tenant-scoped Party rows to the common global-search envelope.
 */
@Service
public class PartySearchService {
    static final String TYPE = "PARTY";

    private final PartySearchRepository repository;

    public PartySearchService(PartySearchRepository repository) {
        this.repository = repository;
    }

    public SearchResponse search(UUID tenantId, String q, Integer limit) {
        SearchQuery query = SearchQuery.of(q, limit);
        List<SearchItem> items = repository.search(tenantId, query.term(), query.limit()).stream()
                .map(PartySearchService::toItem)
                .toList();
        return new SearchResponse(query.term(), query.limit(), items);
    }

    private static SearchItem toItem(PartySearchRow row) {
        return new SearchItem(
                row.id(),
                TYPE,
                row.partyNumber(),
                row.displayName() + " \u00B7 " + row.partyType(),
                row.status(),
                "/party?record=" + row.id()
        );
    }
}
