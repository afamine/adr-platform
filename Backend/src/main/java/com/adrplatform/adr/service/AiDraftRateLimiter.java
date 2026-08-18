package com.adrplatform.adr.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Limits external AI calls per authenticated user without affecting normal ADR operations. */
@Component
public class AiDraftRateLimiter {

    private static final int REQUESTS_PER_MINUTE = 5;
    private final ConcurrentHashMap<UUID, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(UUID userId) {
        return buckets.computeIfAbsent(userId, ignored -> newBucket()).tryConsume(1);
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        REQUESTS_PER_MINUTE,
                        Refill.greedy(REQUESTS_PER_MINUTE, Duration.ofMinutes(1))))
                .build();
    }
}
