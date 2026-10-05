package com.gorkemuysal.minibank.transfer.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountServiceClient {

    /**
     * Status codes that definitively mean "rejected for a business reason, nothing happened".
     * Everything else is treated as Unknown, because we cannot be sure the money didn't move,
     * or because the problem is on our side (e.g. 401/403 = misconfiguration) and a retry may succeed.
     */
    private static final Set<HttpStatus> BUSINESS_REJECTIONS = Set.of(
            HttpStatus.BAD_REQUEST,
            HttpStatus.NOT_FOUND,
            HttpStatus.UNPROCESSABLE_CONTENT);

    private final RestClient accountServiceRestClient;

    public PostingOutcome post(PostingRequest request) {
        try {
            accountServiceRestClient.post()
                    .uri("/internal/v1/postings")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            return new PostingOutcome.Posted();

        } catch (RestClientResponseException e) {
            // account-service answered with a non-2xx status.
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status != null && BUSINESS_REJECTIONS.contains(status)) {
                return new PostingOutcome.Rejected(readErrorCode(e));
            }
            log.warn("account-service returned {} for transfer {}", e.getStatusCode(), request.transferId());
            return new PostingOutcome.Unknown("HTTP_" + e.getStatusCode().value());

        } catch (RestClientException e) {
            // No response at all: timeout, connection refused, etc.
            log.warn("account-service call failed for transfer {}: {}", request.transferId(), e.getMessage());
            return new PostingOutcome.Unknown("NO_RESPONSE");

        } catch (OAuth2AuthorizationException e) {
            // Could not get a token from Keycloak; the request was never sent.
            log.error("Could not obtain token for account-service: {}", e.getMessage());
            return new PostingOutcome.Unknown("TOKEN_UNAVAILABLE");
        }
    }

    /** account-service returns its own ErrorResponse format; we only need the "code" field. */
    private String readErrorCode(RestClientResponseException e) {
        try {
            AccountServiceError body = e.getResponseBodyAs(AccountServiceError.class);
            if (body != null && body.code() != null) {
                return body.code();
            }
        } catch (RuntimeException ignored) {
            // Body was not in the expected format; fall back to the status code.
        }
        return "HTTP_" + e.getStatusCode().value();
    }

    private record AccountServiceError(String code, String message) {
    }
}