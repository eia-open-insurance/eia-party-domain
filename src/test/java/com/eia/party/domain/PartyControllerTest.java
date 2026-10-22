package com.eia.party.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PartyController.class)
class PartyControllerTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID PARTY_ID = UUID.fromString("0192f3a4-8f1c-7c3a-9b2e-6a1d4f8e2b90");
    private static final String URL = "/api/v1/parties";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PartyClient client;

    @Test
    void listReturnsParties() throws Exception {
        when(client.findAll(TENANT)).thenReturn(List.of(party()));

        mvc.perform(get(URL).header("X-EIA-Tenant", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partyNumber").value("PTY-000000001"))
                .andExpect(jsonPath("$[0].displayName").value("Acme Chemical Corporation"));
    }

    @Test
    void missingPartyMapsToStructured404() throws Exception {
        when(client.findById(TENANT, PARTY_ID))
                .thenThrow(PartyApiException.notFound("Party not found: " + PARTY_ID));

        mvc.perform(get(URL + "/" + PARTY_ID).header("X-EIA-Tenant", TENANT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Party not found: " + PARTY_ID));
    }

    @Test
    void missingTenantHeaderMapsToStructured400() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_REQUIRED"))
                .andExpect(jsonPath("$.message").value("X-EIA-Tenant header is required"));

        verifyNoInteractions(client);
    }

    @Test
    void malformedTenantHeaderMapsToStructured400() throws Exception {
        mvc.perform(get(URL).header("X-EIA-Tenant", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_INVALID"))
                .andExpect(jsonPath("$.message").value("X-EIA-Tenant header must be a UUID"));

        verifyNoInteractions(client);
    }

    @Test
    void expertBadRequestMapsToStructured400() throws Exception {
        when(client.create(eq(TENANT), any()))
                .thenThrow(PartyApiException.badRequest("VALIDATION", "partyType is required"));

        mvc.perform(post(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "partyType": "ORGANIZATION",
                                  "displayName": "Acme Chemical Corporation",
                                  "legalName": "Acme Chemical Corporation"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("partyType is required"));
    }

    @Test
    void expertOutageMapsToStructured503() throws Exception {
        when(client.findAll(TENANT)).thenThrow(PartyApiException.expertUnavailable());

        mvc.perform(get(URL).header("X-EIA-Tenant", TENANT))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXPERT_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Party expert is unreachable"));
    }

    @Test
    void expertConflictMapsToStructured409() throws Exception {
        when(client.create(eq(TENANT), any()))
                .thenThrow(PartyApiException.conflict("DUPLICATE_PARTY", "Party already exists"));

        mvc.perform(post(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "partyType": "ORGANIZATION",
                                  "displayName": "Acme Chemical Corporation",
                                  "legalName": "Acme Chemical Corporation"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PARTY"))
                .andExpect(jsonPath("$.message").value("Party already exists"));
    }

    private static PartyDto party() {
        Instant now = Instant.parse("2026-10-19T12:00:00Z");
        return new PartyDto(
                PARTY_ID,
                TENANT,
                "PTY-000000001",
                "ORGANIZATION",
                "Acme Chemical Corporation",
                "Acme Chemical Corporation",
                "ACTIVE",
                "CLEAR",
                "INTERNAL",
                0,
                now,
                now);
    }
}
