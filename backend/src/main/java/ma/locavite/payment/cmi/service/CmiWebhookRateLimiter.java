package ma.locavite.payment.cmi.service;

import ma.locavite.payment.cmi.config.CmiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rate limiter par IP pour l'endpoint /api/payments/cmi/webhook (10 requêtes / minute par IP).
 * Supporte une implémentation distribuée Redis avec repli automatique en mémoire (Bucket / fenêtre glissante).
 */
@Service
public class CmiWebhookRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(CmiWebhookRateLimiter.class);
    private static final String REDIS_KEY_PREFIX = "cmi:webhook:ratelimit:";
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    private final CmiProperties cmiProperties;
    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, Deque<Long>> localBuckets = new ConcurrentHashMap<>();

    public CmiWebhookRateLimiter(CmiProperties cmiProperties) {
        this(cmiProperties, null);
    }

    @Autowired
    public CmiWebhookRateLimiter(CmiProperties cmiProperties, @Nullable StringRedisTemplate redisTemplate) {
        this.cmiProperties = cmiProperties;
        this.redisTemplate = redisTemplate;
    }

    public boolean tryConsume(String clientIp) {
        String key = (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp.trim();
        int maxRequests = Math.max(1, cmiProperties.getWebhookRateLimitPerMinute());

        if (redisTemplate != null) {
            try {
                String redisKey = REDIS_KEY_PREFIX + key;
                Long currentCount = redisTemplate.opsForValue().increment(redisKey);
                if (currentCount != null) {
                    if (currentCount == 1L) {
                        redisTemplate.expire(redisKey, WINDOW_DURATION);
                    }
                    return currentCount <= maxRequests;
                }
            } catch (Exception e) {
                log.debug("Redis indisponible pour le rate limiting CMI, bascule sur le bucket local en mémoire: {}", e.getMessage());
            }
        }

        return tryConsumeLocal(key, maxRequests);
    }

    private boolean tryConsumeLocal(String ip, int maxRequests) {
        long nowMillis = Instant.now().toEpochMilli();
        long windowStartMillis = nowMillis - WINDOW_DURATION.toMillis();

        Deque<Long> timestamps = localBuckets.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStartMillis) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(nowMillis);
            return true;
        }
    }

    public int getRemainingTokens(String clientIp) {
        String key = (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp.trim();
        int maxRequests = Math.max(1, cmiProperties.getWebhookRateLimitPerMinute());
        Deque<Long> timestamps = localBuckets.get(key);
        if (timestamps == null) {
            return maxRequests;
        }
        long windowStartMillis = Instant.now().toEpochMilli() - WINDOW_DURATION.toMillis();
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStartMillis) {
                timestamps.pollFirst();
            }
            return Math.max(0, maxRequests - timestamps.size());
        }
    }

    public void reset() {
        localBuckets.clear();
    }
}
