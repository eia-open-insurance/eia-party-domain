package com.eia.party.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class PartyClient {
    private final RestClient client;

    public PartyClient(RestClient.Builder builder, @Value("${eia.party.expert-url:http://localhost:8081}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public List<PartyDto> findAll(UUID tenantId) {
        return client.get()
                .uri(uri -> uri.path("/internal/v1/parties").queryParam("tenantId", tenantId).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public PartyDto findById(UUID tenantId, UUID id) {
        return client.get()
                .uri(uri -> uri.path("/internal/v1/parties/{id}").queryParam("tenantId", tenantId).build(id))
                .retrieve()
                .body(PartyDto.class);
    }

    public PartyDto create(UUID tenantId, CreatePartyCommand command) {
        return client.post()
                .uri("/internal/v1/parties")
                .body(new CreatePartyRequest(tenantId, command.partyType(), command.displayName(), command.legalName()))
                .retrieve()
                .body(PartyDto.class);
    }

    record CreatePartyRequest(UUID tenantId, String partyType, String displayName, String legalName) {}
}
