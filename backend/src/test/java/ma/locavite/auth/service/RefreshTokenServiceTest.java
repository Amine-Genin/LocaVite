package ma.locavite.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final String PREFIX = "locavite:refresh_token:";
    private static final long EXPIRATION_MILLIS = 120_000L;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(redisTemplate);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", EXPIRATION_MILLIS);
    }

    @Test
    void createRefreshToken_storesUniqueTokenWithConfiguredExpiration() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = refreshTokenService.createRefreshToken("user@example.com");

        assertThat(token).hasSize(64).doesNotContain("-");
        verify(valueOperations).set(PREFIX + token, "user@example.com",
                Duration.ofMillis(EXPIRATION_MILLIS));
        assertThat(refreshTokenService.getRefreshTokenExpiration()).isEqualTo(EXPIRATION_MILLIS);
    }

    @Test
    void createRefreshToken_propagatesRedisFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new IllegalStateException("Redis unavailable"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        assertThatThrownBy(() -> refreshTokenService.createRefreshToken("user@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Redis unavailable");
    }

    @Test
    void validateAndGetEmail_returnsStoredEmail() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(PREFIX + "valid-token")).thenReturn("user@example.com");

        assertThat(refreshTokenService.validateAndGetEmail("valid-token"))
                .contains("user@example.com");
    }

    @Test
    void validateAndGetEmail_returnsEmptyForBlankMissingOrUnreadableToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        assertThat(refreshTokenService.validateAndGetEmail(null)).isEmpty();
        assertThat(refreshTokenService.validateAndGetEmail("  ")).isEmpty();
        when(valueOperations.get(PREFIX + "unknown-token")).thenReturn(null);
        assertThat(refreshTokenService.validateAndGetEmail("unknown-token")).isEmpty();
        when(valueOperations.get(PREFIX + "redis-error"))
                .thenThrow(new IllegalStateException("Redis unavailable"));
        assertThat(refreshTokenService.validateAndGetEmail("redis-error")).isEmpty();
    }

    @Test
    void revokeRefreshToken_deletesTokenAndIgnoresBlankInput() {
        when(redisTemplate.delete(PREFIX + "token")).thenReturn(true);

        refreshTokenService.revokeRefreshToken(null);
        refreshTokenService.revokeRefreshToken(" ");
        refreshTokenService.revokeRefreshToken("token");

        verify(redisTemplate).delete(PREFIX + "token");
    }

    @Test
    void revokeRefreshToken_propagatesRedisFailure() {
        when(redisTemplate.delete(PREFIX + "token"))
                .thenThrow(new IllegalStateException("Redis unavailable"));

        assertThatThrownBy(() -> refreshTokenService.revokeRefreshToken("token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Redis unavailable");
    }
}
