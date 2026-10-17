package ma.locavite.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final String REFRESH_TOKEN_PREFIX = "locavite:refresh_token:";

    private final StringRedisTemplate redisTemplate;

    @Value("${security.jwt.refresh-token.expiration:604800000}")
    private long refreshTokenExpiration; // 7 days (604800000 ms)

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String createRefreshToken(String email) {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String key = buildKey(token);

        try {
            redisTemplate.opsForValue().set(key, email, Duration.ofMillis(refreshTokenExpiration));
            log.info("Refresh token généré pour [{}] avec TTL de {} ms", email, refreshTokenExpiration);
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du refresh token dans Redis : {}", e.getMessage());
            throw e;
        }

        return token;
    }

    public Optional<String> validateAndGetEmail(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String key = buildKey(token);
        try {
            String email = redisTemplate.opsForValue().get(key);
            if (email == null) {
                log.warn("Refresh token invalide ou expiré");
                return Optional.empty();
            }
            return Optional.of(email);
        } catch (Exception e) {
            log.error("Erreur lors de la lecture du refresh token dans Redis : {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void revokeRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        String key = buildKey(token);
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("Révocation du refresh token (supprimé = {})", deleted);
        } catch (Exception e) {
            log.error("Erreur lors de la révocation du refresh token dans Redis : {}", e.getMessage());
            throw e;
        }
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    private String buildKey(String token) {
        return REFRESH_TOKEN_PREFIX + token;
    }
}
