package ma.locavite.payment.cmi.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import ma.locavite.payment.cmi.config.CmiProperties;
import ma.locavite.payment.cmi.exception.CmiUnavailableException;
import ma.locavite.payment.exception.PaymentExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.cache.type=none",
        "management.health.mail.enabled=false"
})
class CmiResilienceTest {

    @Autowired
    private CmiClient cmiClient;

    @Autowired
    private CmiProperties cmiProperties;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @MockBean
    private RestClient cmiRestClient;

    private RestClient.RequestHeadersUriSpec uriSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        cmiProperties.setCheckGatewayHealth(true);
        cmiProperties.setFallbackMode(false);

        // Reset Circuit Breaker and Retry state between tests to avoid cross-test interference
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("cmi");
        cb.reset();
        cb.transitionToClosedState();

        uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(cmiRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("Critère 4 : Retry automatique (3 tentatives) lors d'un timeout CMI")
    void testRetryOnCmiTimeout_performsThreeAttempts() {
        // Simule un timeout réseau vers la passerelle CMI
        when(responseSpec.toBodilessEntity())
                .thenThrow(new ResourceAccessException("Read timed out communicating with CMI gateway"));

        assertThatThrownBy(() -> cmiClient.ensureGatewayAvailable())
                .isInstanceOf(CmiUnavailableException.class)
                .hasMessageContaining("Timeout CMI");

        // Vérifie que l'appel a bien été retenté 3 fois au total (1 appel initial + 2 retries)
        verify(responseSpec, times(3)).toBodilessEntity();
    }

    @Test
    @DisplayName("Critère 4 : Récupération après échec temporaire au 1er appel puis succès au 2ème retry")
    void testRetryRecovery_succeedsOnSecondAttempt() {
        when(responseSpec.toBodilessEntity())
                .thenThrow(new ResourceAccessException("Connection timed out"))
                .thenReturn(ResponseEntity.ok().build());

        // L'appel ne doit pas lever d'exception car le 2ème appel a réussi
        cmiClient.ensureGatewayAvailable();

        verify(responseSpec, times(2)).toBodilessEntity();
    }

    @Test
    @DisplayName("Critère 5 : Circuit breaker ouvert après N échecs consécutifs et rejet immédiat")
    void testCircuitBreakerOpensAfterFailures_rejectsSubsequentCalls() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("cmi");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        when(responseSpec.toBodilessEntity())
                .thenThrow(new ResourceAccessException("Gateway connection failure"));

        // Appel 1 : 3 tentatives échouent (3 appels comptabilisés dans le circuit breaker)
        assertThatThrownBy(() -> cmiClient.ensureGatewayAvailable())
                .isInstanceOf(CmiUnavailableException.class);

        // Appel 2 : les tentatives supplémentaires atteignent le seuil minimumNumberOfCalls=5
        // et font basculer le circuit breaker en OPEN
        assertThatThrownBy(() -> cmiClient.ensureGatewayAvailable())
                .isInstanceOf(Exception.class);

        // Le circuit breaker doit être passé en état OPEN
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Appel 3 : le circuit breaker étant OPEN, l'appel suivant doit être bloqué immédiatement
        // sans contacter la passerelle distante
        reset(responseSpec);
        assertThatThrownBy(() -> cmiClient.ensureGatewayAvailable())
                .isInstanceOf(CallNotPermittedException.class);

        verifyNoInteractions(responseSpec);
    }

    @Test
    @DisplayName("Critère 5 : PaymentExceptionHandler gère CallNotPermittedException en HTTP 503 SERVICE_UNAVAILABLE")
    void testPaymentExceptionHandler_handlesCallNotPermittedException() {
        PaymentExceptionHandler handler = new PaymentExceptionHandler();
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("cmi");
        cb.transitionToOpenState();
        CallNotPermittedException ex = CallNotPermittedException.createCallNotPermittedException(cb);

        ResponseEntity<Map<String, Object>> response = handler.handleCmiUnavailable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("detail").toString()).contains("CircuitBreaker 'cmi' is OPEN");
    }
}
