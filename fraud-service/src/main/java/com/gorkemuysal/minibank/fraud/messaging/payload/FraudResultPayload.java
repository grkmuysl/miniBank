package com.gorkemuysal.minibank.fraud.messaging.payload;

import java.util.List;

/** reasons: triggered rule codes; empty for an approval. */
public record FraudResultPayload(List<String> reasons) {
}