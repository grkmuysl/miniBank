package com.gorkemuysal.minibank.ledger;

import com.gorkemuysal.minibank.ledger.dto.CreatePostingRequest;
import com.gorkemuysal.minibank.ledger.dto.PostingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal API, callable only by transfer-service (ROLE_TRANSFER_SERVICE, see SecurityConfig).
 * Always returns 200 on success, both for the first call and for idempotent replays.
 */
@RestController
@RequestMapping("/internal/v1/postings")
@RequiredArgsConstructor
public class InternalPostingController {

    private final PostingService postingService;

    @PostMapping
    public PostingResponse post(@Valid @RequestBody CreatePostingRequest request) {
        try {
            return postingService.post(request);
        } catch (DataIntegrityViolationException e) {
            // Last line of defense: a concurrent call with the same transferId won the race
            // (only possible if it used different IBANs and therefore different locks).
            // post()'s transaction is already rolled back here, so this read runs cleanly.
            return postingService.findPosted(request.transferId()).orElseThrow(() -> e);
        }
    }
}