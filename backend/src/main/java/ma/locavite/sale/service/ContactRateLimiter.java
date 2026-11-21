package ma.locavite.sale.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class ContactRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ContactRateLimiter.class);
    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String REDIS_KEY_PREFIX = "sale:contact:ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, Deque<Long>> localBuckets = new ConcurrentHashMap<>();

    public ContactRateLimiter(@Nullable StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryConsume(String clientIp) {
        String key = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
        if (redisTemplate != null) {
            try {
                String redisKey = REDIS_KEY_PREFIX + key;
                Long count = redisTemplate.opsForValue().increment(redisKey);
                if (count != null) {
                    if (count == 1L) {
                        redisTemplate.expire(redisKey, WINDOW);
                    }
                    return count <= MAX_REQUESTS_PER_MINUTE;
                }
            } catch (Exception e) {
                log.warn("Redis indisponible pour le rate limiting des demandes de contact, repli local: {}",
                        e.getMessage());
            }
        }
        return tryConsumeLocally(key);
    }

    private boolean tryConsumeLocally(String ip) {
        long now = Instant.now().toEpochMilli();
        long cutoff = now - WINDOW.toMillis();
        Deque<Long> timestamps = localBuckets.computeIfAbsent(ip, ignored -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }
}
