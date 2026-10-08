package com.gorkemuysal.minibank.gateway.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.local.LocalBucketBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Buckets in local memory. Each gateway instance has its own counters,
 * so limits multiply with the number of instances (fixed in part B with Redis).
 */
@Component
public class InMemoryBucketStore implements BucketStore {

    // Bounded on purpose: a plain ConcurrentHashMap would grow with every new IP or user
    // and never shrink, so an attacker rotating IPs could exhaust the gateway's memory.
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    @Override
    public Bucket resolve(String key, BucketConfiguration configuration) {
        return buckets.get(key, k -> newBucket(configuration));
    }

    private static Bucket newBucket(BucketConfiguration configuration) {
        LocalBucketBuilder builder = Bucket.builder();
        for (Bandwidth bandwidth : configuration.getBandwidths()) {
            builder.addLimit(bandwidth);
        }
        return builder.build();
    }
}