package com.adrplatform.auth.security;

import com.adrplatform.auth.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimiter {
    private final StringRedisTemplate redis;

    @Value("${auth.rate-limit.fail-open:false}")
    private boolean failOpen;

    public void check(String action, String ip, String account, int ipLimit, int accountLimit, Duration window) {
        if (!consume("ip", action, ip, ipLimit, window) || !consume("account", action, account, accountLimit, window)) {
            throw new RateLimitExceededException();
        }
    }

    public boolean consume(String scope, String action, String subject, int limit, Duration window) {
        if (subject == null || subject.isBlank()) return true;
        String key = "adr:rate:" + scope + ":" + action + ":" + subject.trim().toLowerCase();
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, window);
            }
            return count != null && count <= limit;
        } catch (DataAccessException ex) {
            log.error("Redis rate limiting unavailable for action {}", action, ex);
            if (failOpen) return true;
            throw new RateLimitExceededException();
        }
    }
}
