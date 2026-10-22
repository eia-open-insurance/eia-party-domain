package com.eia.party.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PartyClientTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID PARTY_ID = UUID.fromString("0192f3a4-8f1c-7c3a-9b2e-6a1d4f8e2b90");
    private static final UUID CHILD_ID = UUID.fromString("0192f3a4-9000-7c3a-9b2e-6a1d4f8e2b90");
    private static final String EXPERT = "http://party-expert";

    private MockRestServiceServer server;
    private PartyClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PartyClient(builder, new ObjectMapper(), EXPERT);
    }

    @Test
    void expertNotFoundIsTranslatedToStructuredNotFound() {
        server.expect(requestTo(EXPERT + "/internal/v1/parties/" + PARTY_ID + "?tenantId=" + TENANT))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"NOT_FOUND\",\"message\":\"Party not found: " + PARTY_ID + "\"}"));

        PartyApiException exception = assertThrows(PartyApiException.class,
                () -> client.findById(TENANT, PARTY_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("NOT_FOUND", exception.getCode());
        assertEquals("Party not found: " + PARTY_ID, exception.getMessage());
        server.verify();
    }

    @Test
    void expertBadRequestIsTranslatedToStructuredBadRequest() {
        server.expect(requestTo(EXPERT + "/internal/v1/parties"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"VALIDATION\",\"message\":\"partyType is required\"}"));

        PartyApiException exception = assertThrows(PartyApiException.class,
                () -> client.create(TENANT, new CreatePartyCommand("", "Acme", "Acme")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("VALIDATION", exception.getCode());
        assertEquals("partyType is required", exception.getMessage());
        server.verify();
    }

    @Test
    void expertServerErrorIsTranslatedToStructuredUnavailable() {
        server.expect(requestTo(EXPERT + "/internal/v1/parties?tenantId=" + TENANT))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"EXPERT_UNAVAILABLE\",\"message\":\"down\"}"));

        PartyApiException exception = assertThrows(PartyApiException.class,
                () -> client.findAll(TENANT));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertEquals("EXPERT_UNAVAILABLE", exception.getCode());
        assertEquals("Party expert is unreachable", exception.getMessage());
        server.verify();
    }

    @Test
    void findIdentifiersForwardsTenantAndMapsTheExpertProjection() {
        server.expect(requestTo(EXPERT + "/internal/v1/parties/" + PARTY_ID
                        + "/identifiers?tenantId=" + TENANT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": "%s",
                            "tenantId": "%s",
                            "partyId": "%s",
                            "identifierType": "TAX_ID",
                            "identifierValue": "123-45-6789",
                            "issuingAuthority": "IRS",
                            "effectiveDate": "2026-01-15",
                            "createdAt": "2026-10-22T09:00:00Z",
                            "createdBy": "system"
                          }
                        ]
                        """.formatted(CHILD_ID, TENANT, PARTY_ID), MediaType.APPLICATION_JSON));

        var identifiers = client.findIdentifiers(TENANT, PARTY_ID);

        assertEquals(1, identifiers.size());
        PartyIdentifierDto identifier = identifiers.getFirst();
        assertEquals(CHILD_ID, identifier.id());
        assertEquals(TENANT, identifier.tenantId());
        assertEquals(PARTY_ID, identifier.partyId());
        assertEquals("TAX_ID", identifier.identifierType());
        assertEquals("123-45-6789", identifier.identifierValue());
        assertEquals("IRS", identifier.issuingAuthority());
        assertEquals(LocalDate.parse("2026-01-15"), identifier.effectiveDate());
        assertEquals(Instant.parse("2026-10-22T09:00:00Z"), identifier.createdAt());
        assertEquals("system", identifier.createdBy());
        server.verify();
    }

    @Test
    void createIdentifierForwardsTheBodyUnchangedWithTheHeaderTenant() {
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.put("identifierType", "TAX_ID");
        body.put("identifierValue", "123-45-6789");
        body.put("issuingAuthority", "IRS");
        body.put("effectiveDate", "2026-01-15");

        server.expect(requestTo(EXPERT + "/internal/v1/parties/" + PARTY_ID
                        + "/identifiers?tenantId=" + TENANT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(content().json("""
                        {
                          "identifierType": "TAX_ID",
                          "identifierValue": "123-45-6789",
                          "issuingAuthority": "IRS",
                          "effectiveDate": "2026-01-15"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "id": "%s",
                          "tenantId": "%s",
                          "partyId": "%s",
                          "identifierType": "TAX_ID",
                          "identifierValue": "123-45-6789",
                          "issuingAuthority": "IRS",
                          "effectiveDate": "2026-01-15",
                          "createdAt": "2026-10-22T09:00:00Z",
                          "createdBy": "system"
                        }
                        """.formatted(CHILD_ID, TENANT, PARTY_ID), MediaType.APPLICATION_JSON));

        PartyIdentifierDto identifier = client.createIdentifier(TENANT, PARTY_ID, body);

        assertEquals(CHILD_ID, identifier.id());
        assertEquals("TAX_ID", identifier.identifierType());
        assertEquals("123-45-6789", identifier.identifierValue());
        assertEquals("IRS", identifier.issuingAuthority());
        assertEquals(LocalDate.parse("2026-01-15"), identifier.effectiveDate());
        assertEquals("system", identifier.createdBy());
        server.verify();
    }

    @Test
    void createIdentifierPreservesExpertConflict() {
        server.expect(requestTo(EXPERT + "/internal/v1/parties/" + PARTY_ID
                        + "/identifiers?tenantId=" + TENANT))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "code": "CONFLICT",
                                  "message": "Identifier already exists for this party: TAX_ID 123-45-6789"
                                }
                                """));

        PartyApiException exception = assertThrows(PartyApiException.class,
                () -> client.createIdentifier(TENANT, PARTY_ID,
                        JsonNodeFactory.instance.objectNode().put("identifierType", "TAX_ID")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("CONFLICT", exception.getCode());
        assertEquals("Identifier already exists for this party: TAX_ID 123-45-6789",
                exception.getMessage());
        server.verify();
    }

    @Test
    void findIdentifiersPreservesExpertNotFound() {
        server.expect(requestTo(EXPERT + "/internal/v1/parties/" + PARTY_ID
                        + "/identifiers?tenantId=" + TENANT))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"NOT_FOUND\",\"message\":\"Party not found: " + PARTY_ID + "\"}"));

        PartyApiException exception = assertThrows(PartyApiException.class,
                () -> client.findIdentifiers(TENANT, PARTY_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("NOT_FOUND", exception.getCode());
        assertEquals("Party not found: " + PARTY_ID, exception.getMessage());
        server.verify();
    }

    @Test
    void findAddressesForwardsTenantAndMapsTheExpertProjection() {
        server.expect(requestTo(EXPERT + "/internal/v1/parties/" + PARTY_ID
                        + "/addresses?tenantId=" + TENANT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": "%s",
                            "tenantId": "%s",
                            "partyId": "%s",
                            "addressType": "PRIMARY",
                            "line1": "1 Infinite Loop",
                            "line2": "Suite 100",
                            "city": "Cupertino",
                            "stateRegion": "CA",
                            "postalCode": "95014",
                            "country": "US",
                            "effectiveDate": "2026-02-01",
                            "createdAt": "2026-10-22T09:00:00Z",
                            "createdBy": "system"
                          }
                        ]
                        """.formatted(CHILD_ID, TENANT, PARTY_ID), MediaType.APPLICATION_JSON));

        var addresses = client.findAddresses(TENANT, PARTY_ID);

        assertEquals(1, addresses.size());
        PartyAddressDto address = addresses.getFirst();
        assertEquals(CHILD_ID, address.id());
        assertEquals(TENANT, address.tenantId());
        assertEquals(PARTY_ID, address.partyId());
        assertEquals("PRIMARY", address.addressType());
        assertEquals("1 Infinite Loop", address.line1());
        assertEquals("Suite 100", address.line2());
        assertEquals("Cupertino", address.city());
        assertEquals("CA", address.stateRegion());
        assertEquals("95014", address.postalCode());
        assertEquals("US", address.country());
        assertEquals(LocalDate.parse("2026-02-01"), address.effectiveDate());
        assertEquals(Instant.parse("2026-10-22T09:00:00Z"), address.createdAt());
        assertEquals("system", address.createdBy());
        server.verify();
    }

    @Test
    void createAddressForwardsTheBodyUnchangedWithTheHeaderTenant() {
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.put("addressType", "PRIMARY");
        body.put("line1", "1 Infinite Loop");
        body.put("city", "Cupertino");
        body.put("stateRegion", "CA");
        body.put("postalCode", "95014");
        body.put("country", "US");

        server.expect(requestTo(EXPERT + "/internal/v1/parties/" + PARTY_ID
                        + "/addresses?tenantId=" + TENANT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(content().json("""
                        {
                          "addressType": "PRIMARY",
                          "line1": "1 Infinite Loop",
                          "city": "Cupertino",
                          "stateRegion": "CA",
                          "postalCode": "95014",
                          "country": "US"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "id": "%s",
                          "tenantId": "%s",
                          "partyId": "%s",
                          "addressType": "PRIMARY",
                          "line1": "1 Infinite Loop",
                          "city": "Cupertino",
                          "stateRegion": "CA",
                          "postalCode": "95014",
                          "country": "US",
                          "createdAt": "2026-10-22T09:00:00Z",
                          "createdBy": "system"
                        }
                        """.formatted(CHILD_ID, TENANT, PARTY_ID), MediaType.APPLICATION_JSON));

        PartyAddressDto address = client.createAddress(TENANT, PARTY_ID, body);

        assertEquals(CHILD_ID, address.id());
        assertEquals("PRIMARY", address.addressType());
        assertEquals("1 Infinite Loop", address.line1());
        assertEquals("Cupertino", address.city());
        assertEquals("US", address.country());
        assertEquals("system", address.createdBy());
        server.verify();
    }

    @Test
    void createAddressPreservesExpertConflict() {
        server.expect(requestTo(EXPERT + "/internal/v1/parties/" + PARTY_ID
                        + "/addresses?tenantId=" + TENANT))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "code": "CONFLICT",
                                  "message": "Address already exists for this party"
                                }
                                """));

        PartyApiException exception = assertThrows(PartyApiException.class,
                () -> client.createAddress(TENANT, PARTY_ID,
                        JsonNodeFactory.instance.objectNode().put("addressType", "PRIMARY")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("CONFLICT", exception.getCode());
        assertEquals("Address already exists for this party", exception.getMessage());
        server.verify();
    }
}
