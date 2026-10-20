package com.eia.party.domain;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Read-only, tenant-scoped queries behind the Party global search.
 *
 * <p>Matching is a case-insensitive contains over {@code party_number},
 * {@code display_name}, and {@code legal_name}. A blank term returns the most
 * recently updated parties. Ordering is stable: {@code updated_at DESC,
 * id ASC}.</p>
 */
@Repository
public class PartySearchRepository {
    private static final String SELECT = """
            SELECT id, party_number, display_name, party_type, status
              FROM party
            """;

    private static final RowMapper<PartySearchRow> MAPPER = (rs, rowNum) -> new PartySearchRow(
            rs.getObject("id", UUID.class),
            rs.getString("party_number"),
            rs.getString("display_name"),
            rs.getString("party_type"),
            rs.getString("status")
    );

    private final JdbcTemplate jdbc;

    public PartySearchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PartySearchRow> search(UUID tenantId, String term, int limit) {
        if (term == null || term.isBlank()) {
            return jdbc.query(SELECT + """
                     WHERE tenant_id = ?
                     ORDER BY updated_at DESC, id ASC
                     LIMIT ?
                    """, MAPPER, tenantId, limit);
        }

        String pattern = containsPattern(term);
        return jdbc.query(SELECT + """
                 WHERE tenant_id = ?
                   AND (LOWER(party_number) LIKE ? ESCAPE '!'
                        OR LOWER(display_name) LIKE ? ESCAPE '!'
                        OR LOWER(legal_name) LIKE ? ESCAPE '!')
                 ORDER BY updated_at DESC, id ASC
                 LIMIT ?
                """, MAPPER, tenantId, pattern, pattern, pattern, limit);
    }

    /**
     * Builds a contains pattern with {@code !} as the escape character so a
     * caller-supplied {@code %}, {@code _}, or {@code !} is matched literally.
     */
    static String containsPattern(String term) {
        String escaped = term.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
