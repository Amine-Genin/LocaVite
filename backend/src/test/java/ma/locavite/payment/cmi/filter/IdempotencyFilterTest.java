package ma.locavite.payment.cmi.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.locavite.payment.cmi.controller.CmiPaymentController;
import ma.locavite.payment.cmi.domain.IdempotencyKey;
import ma.locavite.payment.cmi.dto.CmiInitiateRequest;
import ma.locavite.payment.cmi.dto.CmiInitiateResponse;
import ma.locavite.payment.cmi.repository.IdempotencyKeyRepository;
import ma.locavite.payment.cmi.service.CmiPaymentService;
import ma.locavite.payment.exception.PaymentExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    private MockMvc mockMvc;

    @Mock
    private CmiPaymentService cmiPaymentService;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private IdempotencyFilter idempotencyFilter;

    private final Map<String, IdempotencyKey> simulatedDb = new HashMap<>();

    @BeforeEach
    void setUp() {
        simulatedDb.clear();

        idempotencyFilter = new IdempotencyFilter(idempotencyKeyRepository, objectMapper, 24);

        CmiPaymentController controller = new CmiPaymentController(cmiPaymentService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PaymentExceptionHandler())
                .addFilters(idempotencyFilter)
                .build();
    }

    @Test
    @DisplayName("Critère 1 : Header Idempotency-Key manquant sur POST /initiate → 400 Bad Request")
    void initiate_missingIdempotencyKey_returns400BadRequest() throws Exception {
        CmiInitiateRequest request = new CmiInitiateRequest(
                10L,
                new BigDecimal("500.00"),
                "https://locavite.ma/confirmation"
        );

        mockMvc.perform(post("/api/payments/cmi/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail", containsString("Le header 'Idempotency-Key' est obligatoire")));

        verifyNoInteractions(cmiPaymentService);
    }

    @Test
    @DisplayName("Critère 1 : Header Idempotency-Key vide sur POST /initiate → 400 Bad Request")
    void initiate_blankIdempotencyKey_returns400BadRequest() throws Exception {
        CmiInitiateRequest request = new CmiInitiateRequest(
                10L,
                new BigDecimal("500.00"),
                "https://locavite.ma/confirmation"
        );

        mockMvc.perform(post("/api/payments/cmi/initiate")
                        .header("Idempotency-Key", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Le header 'Idempotency-Key' est obligatoire")));

        verifyNoInteractions(cmiPaymentService);
    }

    @Test
    @DisplayName("Critère 2 & 3 : 2 requêtes avec la même clé → 1 seul paiement créé, 2ème appel retourne le cache (200)")
    void initiate_sameKeyTwice_createsOnlyOnePaymentAndReturnsCachedResponse() throws Exception {
        String key = "IDEMP-KEY-TEST-12345";
        CmiInitiateRequest request = new CmiInitiateRequest(
                10L,
                new BigDecimal("750.00"),
                "https://locavite.ma/confirmation"
        );

        CmiInitiateResponse response = new CmiInitiateResponse(
                100L,
                "PAY-CMI-100",
                new BigDecimal("750.00"),
                "MAD",
                "https://testpayment.cmi.co.ma/fim/est3Dgate",
                "https://testpayment.cmi.co.ma/fim/est3Dgate?oid=PAY-CMI-100",
                Map.of("clientid", "locavite_merchant", "amount", "750.00"),
                Instant.now().plusSeconds(900)
        );

        when(cmiPaymentService.initiate(any(), any())).thenReturn(response);

        // Simulation de la persistance en base dans le mock repository
        when(idempotencyKeyRepository.findByKey(key)).thenAnswer(inv -> Optional.ofNullable(simulatedDb.get(key)));
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class))).thenAnswer(inv -> {
            IdempotencyKey saved = inv.getArgument(0);
            saved.setCreatedAt(Instant.now());
            simulatedDb.put(saved.getKey(), saved);
            return saved;
        });

        // 1er appel : exécute le paiement, enregistre en base et retourne 200
        mockMvc.perform(post("/api/payments/cmi/initiate")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(100))
                .andExpect(jsonPath("$.paymentCode").value("PAY-CMI-100"));

        assertThat(simulatedDb).containsKey(key);
        assertThat(simulatedDb.get(key).getResponse()).contains("PAY-CMI-100");

        // 2ème appel : retourne la réponse en cache sans rappeler cmiPaymentService
        mockMvc.perform(post("/api/payments/cmi/initiate")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.paymentId").value(100))
                .andExpect(jsonPath("$.paymentCode").value("PAY-CMI-100"));

        // Vérification stricte : le service de paiement n'a été appelé qu'une seule et unique fois
        verify(cmiPaymentService, times(1)).initiate(any(), any());
    }

    @Test
    @DisplayName("Critère : clé expirée (> 24h) → réévaluation et nouvel appel au service")
    void initiate_expiredKey_reExecutesPayment() throws Exception {
        String key = "IDEMP-KEY-EXPIRED-999";
        CmiInitiateRequest request = new CmiInitiateRequest(
                10L,
                new BigDecimal("500.00"),
                "https://locavite.ma/confirmation"
        );

        // Clé créée il y a 25 heures (> TTL 24h)
        IdempotencyKey oldKey = new IdempotencyKey(key, "{\"paymentId\":99,\"paymentCode\":\"PAY-OLD\"}", 200, "application/json");
        oldKey.setCreatedAt(Instant.now().minus(25, ChronoUnit.HOURS));
        simulatedDb.put(key, oldKey);

        when(idempotencyKeyRepository.findByKey(key)).thenAnswer(inv -> Optional.ofNullable(simulatedDb.get(key)));
        doAnswer(inv -> {
            simulatedDb.remove(key);
            return null;
        }).when(idempotencyKeyRepository).delete(any(IdempotencyKey.class));

        CmiInitiateResponse newResponse = new CmiInitiateResponse(
                101L,
                "PAY-NEW-101",
                new BigDecimal("500.00"),
                "MAD",
                "https://testpayment.cmi.co.ma/fim/est3Dgate",
                "https://testpayment.cmi.co.ma/fim/est3Dgate?oid=PAY-NEW-101",
                Map.of("clientid", "locavite_merchant"),
                Instant.now().plusSeconds(900)
        );
        when(cmiPaymentService.initiate(any(), any())).thenReturn(newResponse);

        mockMvc.perform(post("/api/payments/cmi/initiate")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(101))
                .andExpect(jsonPath("$.paymentCode").value("PAY-NEW-101"));

        verify(idempotencyKeyRepository, times(1)).delete(oldKey);
        verify(cmiPaymentService, times(1)).initiate(any(), any());
    }
}
