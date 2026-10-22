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

@WebMvcTest(PartyIdentifierController.class)
class PartyIdentifierControllerTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID PARTY_ID = UUID.fromString("0192f3a4-8f1c-7c3a-9b2e-6a1d4f8e2b90");
    private static final UUID IDENTIFIER_ID = UUID.fromString("0192f3a4-9000-7c3a-9b2e-6a1d4f8e2b90");
    private static final String URL = "/api/v1/parties/" + PARTY_ID + "/identifiers";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PartyClient client;

    @Test
    void listsIdentifiersForTheParty() throws Exception {
        when(client.findIdentifiers(TENANT, PARTY_ID)).thenReturn(List.of(identifier()));

        mvc.perform(get(URL).header("X-EIA-Tenant", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(IDENTIFIER_ID.toString()))
                .andExpect(jsonPath("$[0].tenantId").value(TENANT.toString()))
                .andExpect(jsonPath("$[0].partyId").value(PARTY_ID.toString()))
                .andExpect(jsonPath("$[0].identifierType").value("TAX_ID"))
                .andExpect(jsonPath("$[0].identifierValue").value("123-45-6789"))
                .andExpect(jsonPath("$[0].issuingAuthority").value("IRS"))
                .andExpect(jsonPath("$[0].effectiveDate").value("2026-01-15"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-10-22T09:00:00Z"))
                .andExpect(jsonPath("$[0].createdBy").value("system"));
    }

    @Test
    void createsAnIdentifierAndReturnsCreated() throws Exception {
        when(client.createIdentifier(eq(TENANT), eq(PARTY_ID), any()))
                .thenReturn(identifier());

        mvc.perform(post(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifierType": "TAX_ID",
                                  "identifierValue": "123-45-6789",
                                  "issuingAuthority": "IRS",
                                  "effectiveDate": "2026-01-15"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(IDENTIFIER_ID.toString()))
                .andExpect(jsonPath("$.identifierType").value("TAX_ID"))
                .andExpect(jsonPath("$.identifierValue").value("123-45-6789"))
                .andExpect(jsonPath("$.issuingAuthority").value("IRS"))
                .andExpect(jsonPath("$.effectiveDate").value("2026-01-15"))
                .andExpect(jsonPath("$.createdBy").value("system"));

        ArgumentCaptor<ObjectNode> body = ArgumentCaptor.forClass(ObjectNode.class);
        verify(client).createIdentifier(eq(TENANT), eq(PARTY_ID), body.capture());
        assertThat(body.getValue().path("identifierType").asText()).isEqualTo("TAX_ID");
        assertThat(body.getValue().path("identifierValue").asText()).isEqualTo("123-45-6789");
        assertThat(body.getValue().path("issuingAuthority").asText()).isEqualTo("IRS");
    }

    @Test
    void requiresTheTenantHeader() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_REQUIRED"))
                .andExpect(jsonPath("$.message").value("X-EIA-Tenant header is required"));

        verifyNoInteractions(client);
    }

    @Test
    void rejectsAMalformedPartyId() throws Exception {
        mvc.perform(get("/api/v1/parties/not-a-uuid/identifiers")
                        .header("X-EIA-Tenant", TENANT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("Invalid value for partyId"));

        verifyNoInteractions(client);
    }

    @Test
    void preservesExpertValidationProblem() throws Exception {
        when(client.createIdentifier(eq(TENANT), eq(PARTY_ID), any()))
                .thenThrow(PartyApiException.badRequest("VALIDATION_ERROR",
                        "Unsupported identifierType: NATIONAL_ID"));

        mvc.perform(post(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifierType\":\"NATIONAL_ID\",\"identifierValue\":\"X1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Unsupported identifierType: NATIONAL_ID"));
    }

    @Test
    void preservesExpertNotFoundProblem() throws Exception {
        when(client.findIdentifiers(TENANT, PARTY_ID))
                .thenThrow(PartyApiException.notFound("Party not found: " + PARTY_ID));

        mvc.perform(get(URL).header("X-EIA-Tenant", TENANT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Party not found: " + PARTY_ID));
    }

    @Test
    void preservesExpertConflictProblem() throws Exception {
        when(client.createIdentifier(eq(TENANT), eq(PARTY_ID), any()))
                .thenThrow(PartyApiException.conflict("CONFLICT",
                        "Identifier already exists for this party: TAX_ID 123-45-6789"));

        mvc.perform(post(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifierType\":\"TAX_ID\",\"identifierValue\":\"123-45-6789\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message")
                        .value("Identifier already exists for this party: TAX_ID 123-45-6789"));
    }

    @Test
    void rejectsMalformedJsonBeforeCallingTheExpert() throws Exception {
        mvc.perform(post(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));

        verifyNoInteractions(client);
    }

    @Test
    void rejectsAJsonArrayBodyBeforeCallingTheExpert() throws Exception {
        mvc.perform(post(URL)
                        .header("X-EIA-Tenant", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));

        verifyNoInteractions(client);
    }

    private static PartyIdentifierDto identifier() {
        return new PartyIdentifierDto(
                IDENTIFIER_ID,
                TENANT,
                PARTY_ID,
                "TAX_ID",
                "123-45-6789",
                "IRS",
                LocalDate.parse("2026-01-15"),
                Instant.parse("2026-10-22T09:00:00Z"),
                "system");
    }
}
