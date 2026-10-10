package com.gorkemuysal.minibank.transfer;

/** Temporary switch between the old synchronous flow and the new saga. Removed in 5.4. */
public enum TransferFlow {
    SYNC,
    SAGA
}