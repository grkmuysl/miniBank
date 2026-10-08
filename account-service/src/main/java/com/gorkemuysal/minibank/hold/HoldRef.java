package com.gorkemuysal.minibank.hold;

/** Lightweight, unmanaged view of a hold: read BEFORE taking locks without caching the entity. */
public record HoldRef(Long accountId, Long targetAccountId, HoldStatus status) {
}