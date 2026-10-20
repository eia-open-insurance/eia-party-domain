package com.eia.party.domain;

/**
 * Normalized search input: the term is trimmed and the limit is defaulted to
 * 10 and capped at 25 so every vertical returns the same envelope.
 */
record SearchQuery(String term, int limit) {
    static final int DEFAULT_LIMIT = 10;
    static final int MAX_LIMIT = 25;

    static SearchQuery of(String q, Integer limit) {
        String term = q == null ? "" : q.trim();
        int requested = limit == null ? DEFAULT_LIMIT : limit;
        if (requested < 1) {
            throw PartyApiException.badRequest("VALIDATION", "limit must be at least 1");
        }
        return new SearchQuery(term, Math.min(requested, MAX_LIMIT));
    }
}
