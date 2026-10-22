package com.eia.party.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class PartyClient {
    private static final String TENANT_HEADER = "X-EIA-Tenant";

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

    /**
     * Applies one party status transition. The expert owns the status vocabulary and the
     * transition rules; the domain forwards the tenant on both the query parameter and the
     * {@code X-EIA-Tenant} header, passes the optional {@code If-Match} and
     * {@code Idempotency-Key} headers through unchanged, and returns the updated party.
     */
    public PartyDto updateStatus(UUID tenantId, UUID partyId, UpdatePartyStatusCommand command,
                                 String ifMatch, String idempotencyKey) {
        try {
            RestClient.RequestBodySpec spec = client.patch()
                    .uri(uri -> uri.path("/internal/v1/parties/{partyId}/status")
                            .queryParam("tenantId", tenantId)
                            .build(partyId))
                    .header(TENANT_HEADER, tenantId.toString());
            if (ifMatch != null && !ifMatch.isBlank()) {
                spec = spec.header(HttpHeaders.IF_MATCH, ifMatch);
            }
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                spec = spec.header("Idempotency-Key", idempotencyKey);
            }
            PartyDto party = spec
                    .body(new UpdateStatusRequest(command.status(), command.reason()))
                    .retrieve()
                    .body(PartyDto.class);
            if (party == null) {
                throw PartyApiException.expertBadResponse();
            }
            return party;
        } catch (RestClientResponseException ex) {
            throw translate(ex, partyId);
        } catch (ResourceAccessException ex) {
            throw PartyApiException.expertUnavailable();
        } catch (RestClientException ex) {
            throw PartyApiException.expertUnavailable();
        }
    }

    /**
     * Lists one party's identifiers. The expert owns the tenant wall and the ordering; the domain
     * forwards the tenant on both the query parameter and the {@code X-EIA-Tenant} header (the
     * expert rejects a mismatch) and returns the expert projection as the public DTO.
     */
    public List<PartyIdentifierDto> findIdentifiers(UUID tenantId, UUID partyId) {
        try {
            List<PartyIdentifierDto> identifiers = client.get()
                    .uri(uri -> uri.path("/internal/v1/parties/{partyId}/identifiers")
                            .queryParam("tenantId", tenantId)
                            .build(partyId))
                    .header(TENANT_HEADER, tenantId.toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (identifiers == null) {
                throw PartyApiException.expertBadResponse();
            }
            return identifiers.stream().filter(Objects::nonNull).toList();
        } catch (RestClientResponseException ex) {
            throw translate(ex, partyId);
        } catch (ResourceAccessException ex) {
            throw PartyApiException.expertUnavailable();
        } catch (RestClientException ex) {
            throw PartyApiException.expertUnavailable();
        }
    }

    /**
     * Adds an identifier to a party. The body is forwarded unchanged: the expert owns identifier
     * validation, normalization, and duplicate detection, and the tenant comes from the header.
     */
    public PartyIdentifierDto createIdentifier(UUID tenantId, UUID partyId, JsonNode body) {
        try {
            PartyIdentifierDto identifier = client.post()
                    .uri(uri -> uri.path("/internal/v1/parties/{partyId}/identifiers")
                            .queryParam("tenantId", tenantId)
                            .build(partyId))
                    .header(TENANT_HEADER, tenantId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(PartyIdentifierDto.class);
            if (identifier == null) {
                throw PartyApiException.expertBadResponse();
            }
            return identifier;
        } catch (RestClientResponseException ex) {
            throw translate(ex, partyId);
        } catch (ResourceAccessException ex) {
            throw PartyApiException.expertUnavailable();
        } catch (RestClientException ex) {
            throw PartyApiException.expertUnavailable();
        }
    }

    /**
     * Applies an update to one identifier. The body is forwarded unchanged: the expert owns
     * identifier validation, normalization, and duplicate detection, and the tenant comes from
     * the query parameter and the {@code X-EIA-Tenant} header. The expert projection of the
     * updated record is returned.
     */
    public PartyIdentifierDto updateIdentifier(UUID tenantId, UUID partyId, UUID identifierId,
                                               JsonNode body) {
        try {
            PartyIdentifierDto identifier = client.patch()
                    .uri(uri -> uri.path("/internal/v1/parties/{partyId}/identifiers/{identifierId}")
                            .queryParam("tenantId", tenantId)
                            .build(partyId, identifierId))
                    .header(TENANT_HEADER, tenantId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(PartyIdentifierDto.class);
            if (identifier == null) {
                throw PartyApiException.expertBadResponse();
            }
            return identifier;
        } catch (RestClientResponseException ex) {
            throw translate(ex, partyId);
        } catch (ResourceAccessException ex) {
            throw PartyApiException.expertUnavailable();
        } catch (RestClientException ex) {
            throw PartyApiException.expertUnavailable();
        }
    }

    /**
     * Deletes one identifier. The expert owns existence, the tenant wall, and any refusal rule;
     * its {@code 400}/{@code 404}/{@code 409} problems are translated without changing the status.
     */
    public void deleteIdentifier(UUID tenantId, UUID partyId, UUID identifierId) {
        try {
            client.delete()
                    .uri(uri -> uri.path("/internal/v1/parties/{partyId}/identifiers/{identifierId}")
                            .queryParam("tenantId", tenantId)
                            .build(partyId, identifierId))
                    .header(TENANT_HEADER, tenantId.toString())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw translate(ex, partyId);
        } catch (ResourceAccessException ex) {
            throw PartyApiException.expertUnavailable();
        } catch (RestClientException ex) {
            throw PartyApiException.expertUnavailable();
        }
    }

    /**
     * Lists one party's addresses. See {@link #findIdentifiers(UUID, UUID)} for the tenant and
     * mapping rules.
     */
    public List<PartyAddressDto> findAddresses(UUID tenantId, UUID partyId) {
        try {
            List<PartyAddressDto> addresses = client.get()
                    .uri(uri -> uri.path("/internal/v1/parties/{partyId}/addresses")
                            .queryParam("tenantId", tenantId)
                            .build(partyId))
                    .header(TENANT_HEADER, tenantId.toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (addresses == null) {
                throw PartyApiException.expertBadResponse();
            }
            return addresses.stream().filter(Objects::nonNull).toList();
        } catch (RestClientResponseException ex) {
            throw translate(ex, partyId);
        } catch (ResourceAccessException ex) {
            throw PartyApiException.expertUnavailable();
        } catch (RestClientException ex) {
            throw PartyApiException.expertUnavailable();
        }
    }

    /**
     * Adds an address to a party. The body is forwarded unchanged: the expert owns address
     * validation, country normalization, and duplicate detection.
     */
    public PartyAddressDto createAddress(UUID tenantId, UUID partyId, JsonNode body) {
        try {
            PartyAddressDto address = client.post()
                    .uri(uri -> uri.path("/internal/v1/parties/{partyId}/addresses")
                            .queryParam("tenantId", tenantId)
                            .build(partyId))
                    .header(TENANT_HEADER, tenantId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(PartyAddressDto.class);
            if (address == null) {
                throw PartyApiException.expertBadResponse();
            }
            return address;
        } catch (RestClientResponseException ex) {
            throw translate(ex, partyId);
        } catch (ResourceAccessException ex) {
            throw PartyApiException.expertUnavailable();
        } catch (RestClientException ex) {
            throw PartyApiException.expertUnavailable();
        }
    }

    /**
     * Applies an update to one address. The body is forwarded unchanged: the expert owns
     * address validation, country normalization, and duplicate detection.
     */
    public PartyAddressDto updateAddress(UUID tenantId, UUID partyId, UUID addressId,
                                         JsonNode body) {
        try {
            PartyAddressDto address = client.patch()
                    .uri(uri -> uri.path("/internal/v1/parties/{partyId}/addresses/{addressId}")
                            .queryParam("tenantId", tenantId)
                            .build(partyId, addressId))
                    .header(TENANT_HEADER, tenantId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(PartyAddressDto.class);
            if (address == null) {
                throw PartyApiException.expertBadResponse();
            }
            return address;
        } catch (RestClientResponseException ex) {
            throw translate(ex, partyId);
        } catch (ResourceAccessException ex) {
            throw PartyApiException.expertUnavailable();
        } catch (RestClientException ex) {
            throw PartyApiException.expertUnavailable();
        }
    }

    /**
     * Deletes one address. See {@link #deleteIdentifier(UUID, UUID, UUID)} for the tenant and
     * error-preservation rules.
     */
    public void deleteAddress(UUID tenantId, UUID partyId, UUID addressId) {
        try {
            client.delete()
                    .uri(uri -> uri.path("/internal/v1/parties/{partyId}/addresses/{addressId}")
                            .queryParam("tenantId", tenantId)
                            .build(partyId, addressId))
                    .header(TENANT_HEADER, tenantId.toString())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw translate(ex, partyId);
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
        if (status == HttpStatus.CONFLICT) {
            String code = error == null || error.code() == null || error.code().isBlank()
                    ? "CONFLICT"
                    : error.code();
            String message = error == null || error.message() == null || error.message().isBlank()
                    ? "Party expert reported a conflict"
                    : error.message();
            return PartyApiException.conflict(code, message);
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

    record UpdateStatusRequest(String status, String reason) {}
}
