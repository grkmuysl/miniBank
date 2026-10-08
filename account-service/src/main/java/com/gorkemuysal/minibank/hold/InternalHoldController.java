package com.gorkemuysal.minibank.hold;

import com.gorkemuysal.minibank.hold.dto.HoldResponse;
import com.gorkemuysal.minibank.hold.dto.PlaceHoldRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * TEMPORARY manual-test API for the hold lifecycle. From 5.2 on these operations are driven
 * by Kafka consumers. Protected by the /internal/** rule (ROLE_TRANSFER_SERVICE).
 */
@RestController
@RequestMapping("/internal/v1/holds")
@RequiredArgsConstructor
public class InternalHoldController {

    private final HoldService holdService;

    @PostMapping
    public HoldResponse place(@Valid @RequestBody PlaceHoldRequest request) {
        return holdService.placeHold(request);
    }

    @PostMapping("/{transferId}/capture")
    public HoldResponse capture(@PathVariable Long transferId) {
        return holdService.capture(transferId);
    }

    @PostMapping("/{transferId}/release")
    public HoldResponse release(@PathVariable Long transferId) {
        return holdService.release(transferId);
    }
}