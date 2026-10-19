package com.eia.party.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class PartyClientTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID PARTY_ID = UUID.fromString("0192f3a4-8f1c-7c3a-9b2e-6a1d4f8e2b90");

    private MockRestServiceServer server;
    private PartyClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PartyClient(builder, new ObjectMapper(), "http://party-expert");
    }

    @Test
    void expertNotFoundIsTranslatedToStructuredNotFound() {
        server.expect(requestTo("http://party-expert/internal/v1/parties/" + PARTY_ID + "?tenantId=" + TENANT))
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
        server.expect(requestTo("http://party-expert/internal/v1/parties"))
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
        server.expect(requestTo("http://party-expert/internal/v1/parties?tenantId=" + TENANT))
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
}
