package com.gorkemuysal.minibank.gateway.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;

/** Where buckets live. In-memory for a single instance; Redis when instances must share limits. */
public interface BucketStore {

    Bucket resolve(String key, BucketConfiguration configuration);
}