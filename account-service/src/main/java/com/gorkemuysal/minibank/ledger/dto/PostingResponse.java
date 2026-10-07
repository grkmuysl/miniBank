package com.gorkemuysal.minibank.ledger.dto;

import com.gorkemuysal.minibank.ledger.Posting;

import java.time.Instant;
import java.util.List;

public record PostingResponse(Long transferId, Instant postedAt) {

    public static PostingResponse from(Long transferId, List<Posting> postings) {
        return new PostingResponse(transferId, postings.getFirst().getCreatedAt());
    }
}