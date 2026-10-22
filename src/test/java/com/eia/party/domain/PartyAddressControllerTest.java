package com.eia.party.domain;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PartyAddressController.class)
class PartyAddressControllerTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID PARTY_ID = UUID.fromString("0192f3a4-8f1c-7c3a-9b2e-6a1d4f8e2b90");
    private static final UUID ADDRESS_ID = UUID.fromString("0192f3a4-a000-7c3a-9b2e-6a1d4f8e2b90");
    private static final String URL = "/api/v1/parties/" + PARTY_ID + "/addresses";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PartyClient client;

    @Test
    void listsAddressesForTheParty() throws Exception {
        when(client.findAddresses(TENANT, PARTY_ID)).thenReturn(List.of(address()));

        mvc.perform(get(URL).header("X-EIA-Tenant", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ADDRESS_ID.toString()))
                .andExpect(jsonPath("$[0].tenantId").value(TENANT.toString()))
                .andExpect(jsonPath("$[0].partyId").value(PARTY_ID.toString()))
                .andExpect(jsonPath("$[0].addressType").value("PRIMARY"))
                .andExpect(jsonPath("$[0].line1").value("1 Infinite Loop"))
                .andExpect(jsonPath("$[0].city").value("Cupertino"))
                .andExpect(jsonPath("$[0].stateRegion").value("CA"))
                .andExpect(jsonPath("$[0].postalCode").value("95014"))
                .andExpect(jsonPath("$[0].country").value("US"))
                .andExpect(jsonPath("$[0].effectiveDate").value("2026-02-01"))
                .andExpect(jsonPath("$[0].createdBy").value("system"));
    }

    @Test
    void createsAnAddressAndReturnsCreated() throws Exception {
        when(client.createAddress(eq(TENANT), eq(PARTY_ID), any())).thenReturn(address());

        mvc.perform(post(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "addressType": "PRIMARY",
                                  "line1": "1 Infinite Loop",
                                  "city": "Cupertino",
                                  "stateRegion": "CA",
                                  "postalCode": "95014",
                                  "country": "US",
                                  "effectiveDate": "2026-02-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ADDRESS_ID.toString()))
                .andExpect(jsonPath("$.addressType").value("PRIMARY"))
                .andExpect(jsonPath("$.line1").value("1 Infinite Loop"))
                .andExpect(jsonPath("$.country").value("US"))
                .andExpect(jsonPath("$.effectiveDate").value("2026-02-01"));

        ArgumentCaptor<ObjectNode> body = ArgumentCaptor.forClass(ObjectNode.class);
        verify(client).createAddress(eq(TENANT), eq(PARTY_ID), body.capture());
        assertThat(body.getValue().path("addressType").asText()).isEqualTo("PRIMARY");
        assertThat(body.getValue().path("postalCode").asText()).isEqualTo("95014");
    }

    @Test
    void requiresTheTenantHeader() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_REQUIRED"));

        verifyNoInteractions(client);
    }

    @Test
    void preservesExpertNotFoundProblem() throws Exception {
        when(client.findAddresses(TENANT, PARTY_ID))
                .thenThrow(PartyApiException.notFound("Party not found: " + PARTY_ID));

        mvc.perform(get(URL).header("X-EIA-Tenant", TENANT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Party not found: " + PARTY_ID));
    }

    @Test
    void preservesExpertConflictProblem() throws Exception {
        when(client.createAddress(eq(TENANT), eq(PARTY_ID), any()))
                .thenThrow(PartyApiException.conflict("CONFLICT",
                        "Address already exists for this party: PRIMARY 1 Infinite Loop"));

        mvc.perform(post(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressType\":\"PRIMARY\",\"country\":\"US\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message")
                        .value("Address already exists for this party: PRIMARY 1 Infinite Loop"));
    }

    @Test
    void preservesExpertValidationProblem() throws Exception {
        when(client.createAddress(eq(TENANT), eq(PARTY_ID), any()))
                .thenThrow(PartyApiException.badRequest("VALIDATION_ERROR",
                        "Unsupported addressType: HOME"));

        mvc.perform(post(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressType\":\"HOME\",\"country\":\"US\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Unsupported addressType: HOME"));
    }

    private static PartyAddressDto address() {
        return new PartyAddressDto(
                ADDRESS_ID,
                TENANT,
                PARTY_ID,
                "PRIMARY",
                "1 Infinite Loop",
                null,
                "Cupertino",
                "CA",
                "95014",
                "US",
                LocalDate.parse("2026-02-01"),
                Instant.parse("2026-10-22T09:00:00Z"),
                "system");
    }
}
