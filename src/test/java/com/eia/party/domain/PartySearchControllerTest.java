package com.eia.party.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PartySearchController.class)
@Import(PartySearchService.class)
class PartySearchControllerTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID PARTY_ID = UUID.fromString("0192f3a4-8f1c-7c3a-9b2e-6a1d4f8e2b90");
    private static final String URL = "/api/v1/parties/search";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PartySearchRepository repository;

    @Test
    void returnsTheCommonSearchEnvelopeAndHref() throws Exception {
        when(repository.search(TENANT, "acme", 10)).thenReturn(List.of(
                new PartySearchRow(PARTY_ID, "PTY-000000001", "Acme Manufacturing",
                        "ORGANIZATION", "ACTIVE")));

        mvc.perform(get(URL).header("X-EIA-Tenant", TENANT).param("q", "acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("acme"))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.items[0].id").value(PARTY_ID.toString()))
                .andExpect(jsonPath("$.items[0].type").value("PARTY"))
                .andExpect(jsonPath("$.items[0].label").value("PTY-000000001"))
                .andExpect(jsonPath("$.items[0].secondary")
                        .value("Acme Manufacturing \u00B7 ORGANIZATION"))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].href")
                        .value("/party?record=" + PARTY_ID));
    }

    @Test
    void blankQueryReturnsTheLatestTenByDefault() throws Exception {
        List<PartySearchRow> rows = IntStream.range(0, 10)
                .mapToObj(i -> new PartySearchRow(
                        UUID.fromString(String.format("00000000-0000-7000-8000-%012d", i + 1)),
                        String.format("PTY-%09d", i + 1), "Party " + i,
                        "ORGANIZATION", "ACTIVE"))
                .toList();
        when(repository.search(TENANT, "", 10)).thenReturn(rows);

        mvc.perform(get(URL).header("X-EIA-Tenant", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value(""))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.items.length()").value(10));
    }

    @Test
    void limitsAboveTheMaximumAreCappedAtTwentyFive() throws Exception {
        when(repository.search(TENANT, "acme", 25)).thenReturn(List.of());

        mvc.perform(get(URL).header("X-EIA-Tenant", TENANT)
                        .param("q", "acme").param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(25));

        verify(repository).search(TENANT, "acme", 25);
    }

    @Test
    void rejectsALimitBelowOne() throws Exception {
        mvc.perform(get(URL).header("X-EIA-Tenant", TENANT)
                        .param("q", "acme").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("limit must be at least 1"));

        verifyNoInteractions(repository);
    }

    @Test
    void trimsTheQueryBeforeSearching() throws Exception {
        when(repository.search(TENANT, "Acme", 10)).thenReturn(List.of());

        mvc.perform(get(URL).header("X-EIA-Tenant", TENANT).param("q", "  Acme  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("Acme"));

        verify(repository).search(TENANT, "Acme", 10);
    }

    @Test
    void requiresTheTenantHeader() throws Exception {
        mvc.perform(get(URL).param("q", "acme"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_REQUIRED"));

        verifyNoInteractions(repository);
    }
}
