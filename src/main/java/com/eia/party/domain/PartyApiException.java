package com.eia.party.domain;

import org.springframework.http.HttpStatus;

/**
 * API-visible Party domain failure carrying the structured error code and HTTP status.
 *
 * <p>The Party domain fronts the expert service, so expert failures are translated into this
 * type instead of escaping as an unhandled 500 (SPEC 6.11, MISMATCHES M-15).</p>
 */
public class PartyApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public PartyApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static PartyApiException notFound(String message) {
        return new PartyApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static PartyApiException badRequest(String code, String message) {
        return new PartyApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static PartyApiException expertUnavailable() {
        return new PartyApiException(HttpStatus.SERVICE_UNAVAILABLE, "EXPERT_UNAVAILABLE",
                "Party expert is unreachable");
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
