package com.gorkemuysal.minibank.transfer.messaging.payload;

import java.util.List;

public record FraudResultPayload(List<String> reasons) {
}