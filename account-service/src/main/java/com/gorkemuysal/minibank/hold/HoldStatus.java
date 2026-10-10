package com.gorkemuysal.minibank.hold;

public enum HoldStatus {
    HELD,       // money reserved on the source account
    CAPTURED,   // money moved to the target account (final)
    RELEASED    // reservation cancelled (final)
}