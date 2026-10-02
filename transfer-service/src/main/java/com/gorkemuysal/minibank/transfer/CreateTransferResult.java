package com.gorkemuysal.minibank.transfer;

/** created: true -> new record (201), false -> return exist record(200). */
public record CreateTransferResult(Transfer transfer, boolean created) {
}