package com.eia.party.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real SQL against H2 in PostgreSQL mode: matching, ordering,
 * blank-term behavior, the limit, and tenant isolation.
 */
class PartySearchRepositoryTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID OTHER_TENANT = UUID.fromString("22222222-2222-7222-8222-222222222222");
    private static final Instant BASE = Instant.parse("2026-10-01T12:00:00Z");

    private JdbcTemplate jdbc;
    private PartySearchRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:party_search_" + UUID.randomUUID().toString().replace("-", "")
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE party (
                    id UUID PRIMARY KEY,
                    tenant_id UUID NOT NULL,
                    party_number VARCHAR(32) NOT NULL,
                    party_type VARCHAR(64) NOT NULL,
                    display_name VARCHAR(300) NOT NULL,
                    legal_name VARCHAR(300) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
        repository = new PartySearchRepository(jdbc);
    }

    @Test
    void matchesPartyNumberDisplayNameAndLegalNameCaseInsensitively() {
        party(1, TENANT, "PTY-000000001", "Acme Manufacturing", "Acme Manufacturing Holdings",
                "ORGANIZATION", "ACTIVE", BASE);
        party(2, TENANT, "PTY-000000002", "Beta Corp", "Beta Corporation",
                "ORGANIZATION", "ACTIVE", BASE.plus(1, ChronoUnit.MINUTES));
        party(3, TENANT, "PTY-000000003", "Gamma Industries", "Delta Holdings LLC",
                "ORGANIZATION", "ACTIVE", BASE.plus(2, ChronoUnit.MINUTES));

        assertThat(ids(repository.search(TENANT, "ACME", 10))).containsExactly(id(1));
        assertThat(ids(repository.search(TENANT, "acme", 10))).containsExactly(id(1));
        assertThat(ids(repository.search(TENANT, "manufactur", 10))).containsExactly(id(1));
        assertThat(ids(repository.search(TENANT, "000000002", 10))).containsExactly(id(2));
        assertThat(ids(repository.search(TENANT, "delta", 10))).containsExactly(id(3));
    }

    @Test
    void blankTermReturnsLatestRecordsByUpdatedAtDescending() {
        for (int i = 1; i <= 12; i++) {
            party(i, TENANT, String.format("PTY-%09d", i), "Party " + i, "Party " + i,
                    "ORGANIZATION", "ACTIVE", BASE.plus(i, ChronoUnit.MINUTES));
        }

        List<PartySearchRow> latestTen = repository.search(TENANT, "   ", 10);

        assertThat(ids(latestTen)).containsExactly(
                id(12), id(11), id(10), id(9), id(8), id(7), id(6), id(5), id(4), id(3));
        assertThat(repository.search(TENANT, "", 25)).hasSize(12);
    }

    @Test
    void appliesTheLimit() {
        for (int i = 1; i <= 8; i++) {
            party(i, TENANT, String.format("PTY-%09d", i), "Shared Name " + i, "Shared Name " + i,
                    "ORGANIZATION", "ACTIVE", BASE.plus(i, ChronoUnit.MINUTES));
        }

        assertThat(repository.search(TENANT, "shared", 3)).hasSize(3);
    }

    @Test
    void keepsResultsTenantScoped() {
        party(1, TENANT, "PTY-SHARED-1", "Shared Name One", "Shared Name One",
                "ORGANIZATION", "ACTIVE", BASE);
        party(2, OTHER_TENANT, "PTY-SHARED-2", "Shared Name Two", "Shared Name Two",
                "ORGANIZATION", "ACTIVE", BASE.plus(1, ChronoUnit.MINUTES));

        assertThat(ids(repository.search(TENANT, "shared", 10))).containsExactly(id(1));
        assertThat(ids(repository.search(TENANT, "", 10))).containsExactly(id(1));
        assertThat(ids(repository.search(OTHER_TENANT, "shared", 10))).containsExactly(id(2));
    }

    @Test
    void treatsSqlWildcardCharactersLiterally() {
        party(1, TENANT, "PTY-000000001", "100% Organic", "100% Organic",
                "ORGANIZATION", "ACTIVE", BASE);
        party(2, TENANT, "PTY-000000002", "100X Organic", "100X Organic",
                "ORGANIZATION", "ACTIVE", BASE.plus(1, ChronoUnit.MINUTES));

        assertThat(ids(repository.search(TENANT, "100%", 10))).containsExactly(id(1));
        assertThat(ids(repository.search(TENANT, "0%0", 10))).isEmpty();
    }

    private void party(int sequence, UUID tenant, String number, String displayName,
                       String legalName, String partyType, String status, Instant updatedAt) {
        jdbc.update("""
                INSERT INTO party (id, tenant_id, party_number, party_type, display_name,
                                   legal_name, status, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id(sequence), tenant, number, partyType, displayName, legalName, status,
                Timestamp.from(updatedAt));
    }

    private static List<UUID> ids(List<PartySearchRow> rows) {
        return rows.stream().map(PartySearchRow::id).toList();
    }

    private static UUID id(int sequence) {
        return UUID.fromString(String.format("00000000-0000-7000-8000-%012d", sequence));
    }
}
