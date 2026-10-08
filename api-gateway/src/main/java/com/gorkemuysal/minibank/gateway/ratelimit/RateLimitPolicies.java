package com.gorkemuysal.minibank.gateway.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;

import java.time.Duration;

/** All rate limits in one place. Keys are prefixed per policy so limits never share a counter. */
public final class RateLimitPolicies {

    /** Before token validation: cheapest place to cut off floods (e.g. invalid-token storms). */
    public static final String IP_PREFIX = "ip:";
    public static final BucketConfiguration IP = perMinute(100);

    /** After token validation: all /api/** requests of one user. */
    public static final String USER_PREFIX = "user:";
    public static final BucketConfiguration USER = perMinute(60);

    /** After token validation: POST /api/v1/transfers of one user. */
    public static final String TRANSFER_PREFIX = "transfer:";
    public static final BucketConfiguration TRANSFER = perMinute(5);

    private static BucketConfiguration perMinute(long requests) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requests)
                        .refillGreedy(requests, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private RateLimitPolicies() {
    }
}