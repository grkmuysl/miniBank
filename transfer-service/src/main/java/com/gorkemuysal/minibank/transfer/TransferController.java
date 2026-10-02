package com.gorkemuysal.minibank.transfer;

import com.gorkemuysal.minibank.transfer.dto.CreateTransferRequest;
import com.gorkemuysal.minibank.transfer.dto.TransferResponse;
import com.gorkemuysal.minibank.transfer.exception.BusinessException;
import com.gorkemuysal.minibank.transfer.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 64;

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateTransferRequest request) {

        if (idempotencyKey.isBlank() || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
        }

        CreateTransferResult result = transferService.create(jwt.getSubject(), idempotencyKey, request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(TransferResponse.from(result.transfer()));
    }

    @GetMapping("/{id}")
    public TransferResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return TransferResponse.from(transferService.getById(id, jwt.getSubject()));
    }

    @GetMapping
    public List<TransferResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return transferService.listForInitiator(jwt.getSubject()).stream()
                .map(TransferResponse::from)
                .toList();
    }
}