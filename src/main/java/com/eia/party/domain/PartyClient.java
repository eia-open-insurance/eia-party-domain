package com.eia.party.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.UUID;

@Component
public class PartyClient {
    private final RestClient client;
    private final ObjectMapper objectMapper;

    public PartyClient(RestClient.Builder builder,
                       ObjectMapper objectMapper,
                       @Value("${eia.party.expert-url:http://localhost:8081}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public List<PartyDto> findAll(UUID tenantId) {
        try {
            return client.get()
                    .uri(uri -> uri.path("/internal/v1/parties").queryParam("tenantId", tenantId).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException ex) {
            throw translate(ex, null);
        } catch (ResourceAccessException ex) {
            throw PartyApiException.expertUnavailable();
        } catch (RestClientException ex) {
            throw PartyApiException.expertUnavailable();
        }
    }

    public PartyDto findById(UUID tenantId, UUID id) {
        try {
            return client.get()
                    .uri(uri -> uri.path("/internal/v1/parties/{id}").queryParam("tenantId", tenantId).build(id))
                    .retrieve()
                    .body(PartyDto.class);
        } catch (RestClientResponseException ex) {
            throw translate(ex, id);
        } catch (ResourceAccessException ex) {
            throw PartyApiException.expertUnavailable();
        } catch (RestClientException ex) {
            throw PartyApiException.expertUnavailable();
        }
    }

    public PartyDto create(UUID tenantId, CreatePartyCommand command) {
        try {
            return client.post()
                    .uri("/internal/v1/parties")
                    .body(new CreatePartyRequest(tenantId, command.partyType(), command.displayName(), command.legalName()))
                    .retrieve()
                    .body(PartyDto.class);
        } catch (RestClientResponseException ex) {
            throw translate(ex, null);
        } catch (ResourceAccessException ex) {
            throw PartyApiException.expertUnavailable();
        } catch (RestClientException ex) {
            throw PartyApiException.expertUnavailable();
        }
    }

    private PartyApiException translate(RestClientResponseException exception, UUID partyId) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        ApiError error = readError(exception);

        if (status == HttpStatus.NOT_FOUND) {
            String message = error == null || error.message() == null || error.message().isBlank()
                    ? "Party not found" + (partyId == null ? "" : ": " + partyId)
                    : error.message();
            return PartyApiException.notFound(message);
        }
        if (status != null && status.is5xxServerError()) {
            return PartyApiException.expertUnavailable();
        }
        if (status != null && status.is4xxClientError()) {
            String code = error == null || error.code() == null || error.code().isBlank()
                    ? "VALIDATION"
                    : error.code();
            String message = error == null || error.message() == null || error.message().isBlank()
                    ? "Party expert rejected the request"
                    : error.message();
            return PartyApiException.badRequest(code, message);
        }
        return PartyApiException.expertUnavailable();
    }

    private ApiError readError(RestClientResponseException exception) {
        try {
            String body = exception.getResponseBodyAsString();
            if (body == null || body.isBlank()) {
                return null;
            }
            return objectMapper.readValue(body, ApiError.class);
        } catch (Exception ex) {
            return null;
        }
    }

    record CreatePartyRequest(UUID tenantId, String partyType, String displayName, String legalName) {}
}
