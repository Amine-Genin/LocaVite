package ma.locavite.payment.cmi.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ma.locavite.payment.cmi.config.CmiProperties;
import ma.locavite.payment.cmi.controller.CmiPaymentController;
import ma.locavite.payment.cmi.domain.WebhookEvent;
import ma.locavite.payment.cmi.repository.WebhookEventRepository;
import ma.locavite.payment.cmi.service.*;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentMethod;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.event.PaymentSucceededEvent;
import ma.locavite.payment.exception.PaymentExceptionHandler;
import ma.locavite.payment.repository.PaymentRepository;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CmiWebhookFilterTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CmiClient cmiClient;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CmiHashService hashService = new CmiHashService();
    private CmiProperties cmiProperties;
    private CmiIpWhitelistService ipWhitelistService;
    private CmiWebhookRateLimiter rateLimiter;
    private CmiSecurityAlertService securityAlertService;
    private CmiWebhookFilter webhookFilter;
    private CmiPaymentService cmiPaymentService;

    private final String storeKey = "cmi_webhook_secret_key_prod_test_999";
    private final Set<String> savedEventIds = ConcurrentHashMap.newKeySet();

    @BeforeEach
    void setUp() {
        savedEventIds.clear();

        cmiProperties = new CmiProperties();
        cmiProperties.setMerchantId("locavite_merchant");
        cmiProperties.setSecretKey(storeKey);
        cmiProperties.setStoreKey(storeKey);
        cmiProperties.setIpWhitelist(List.of("127.0.0.1", "196.200.160.0/24", "41.137.10.20"));
        cmiProperties.setWebhookRateLimitPerMinute(10);
        cmiProperties.setWebhookRequireHttps(false);

        ipWhitelistService = new CmiIpWhitelistService(cmiProperties);
        rateLimiter = new CmiWebhookRateLimiter(cmiProperties);
        securityAlertService = new CmiSecurityAlertService(new SimpleMeterRegistry());

        webhookFilter = new CmiWebhookFilter(
                cmiProperties,
                ipWhitelistService,
                rateLimiter,
                hashService,
                securityAlertService,
                objectMapper
        );

        cmiPaymentService = new CmiPaymentService(
                paymentRepository,
                reservationRepository,
                currentUserService,
                eventPublisher,
                cmiProperties,
                hashService,
                ipWhitelistService,
                cmiClient,
                objectMapper,
                webhookEventRepository,
                rateLimiter,
                securityAlertService
        );

        CmiPaymentController controller = new CmiPaymentController(cmiPaymentService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PaymentExceptionHandler())
                .addFilters(webhookFilter)
                .build();
    }

    @Test
    @DisplayName("Test : webhook signature valide → traitement 200 OK (ACTION=POSTAUTH)")
    void webhook_validSignature_processesPaymentAndReturns200() throws Exception {
        Payment payment = buildPendingPayment(101L, "PAY-SEC001");
        when(paymentRepository.findByCode("PAY-SEC001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(webhookEventRepository.existsByEventId(any())).thenReturn(false);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("event_id", "EVT-1001");
        params.put("oid", "PAY-SEC001");
        params.put("amount", "750.00");
        params.put("Response", "Approved");
        params.put("ProcReturnCode", "00");
        params.put("TransId", "TX-CMI-1001");
        String hash = hashService.generateInitiateHash(params, storeKey, "HMAC_SHA512");

        mockMvc.perform(post("/api/payments/cmi/webhook")
                        .header("X-Forwarded-For", "196.200.160.55")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("event_id", "EVT-1001")
                        .param("oid", "PAY-SEC001")
                        .param("amount", "750.00")
                        .param("Response", "Approved")
                        .param("ProcReturnCode", "00")
                        .param("TransId", "TX-CMI-1001")
                        .param("hash", hash))
                .andExpect(status().isOk())
                .andExpect(content().string("ACTION=POSTAUTH"));

        assertThat(payment.getStatut()).isEqualTo(PaymentStatus.SUCCESS);
        verify(webhookEventRepository, times(1)).save(any(WebhookEvent.class));
        verify(eventPublisher, times(1)).publishEvent(any(PaymentSucceededEvent.class));
    }

    @Test
    @DisplayName("Test : signature invalide → 401 Unauthorized + alerte Prometheus")
    void webhook_invalidSignature_returns401AndTriggersAlert() throws Exception {
        mockMvc.perform(post("/api/payments/cmi/webhook")
                        .header("X-Forwarded-For", "196.200.160.55")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("oid", "PAY-SEC001")
                        .param("amount", "750.00")
                        .param("Response", "Approved")
                        .param("ProcReturnCode", "00")
                        .param("hash", "INVALID_HMAC_SIGNATURE=="))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail", containsString("Signature CMI invalide")));

        assertThat(securityAlertService.getFailureCount("invalid_signature")).isEqualTo(1.0);
        verify(paymentRepository, never()).findByCode(any());
    }

    @Test
    @DisplayName("Test : IP non whitelistée → 403 Forbidden + alerte Prometheus")
    void webhook_nonWhitelistedIp_returns403AndTriggersAlert() throws Exception {
        mockMvc.perform(post("/api/payments/cmi/webhook")
                        .header("X-Forwarded-For", "203.0.113.99")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("oid", "PAY-SEC001")
                        .param("hash", "ANY_HASH"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail", containsString("IP non autorisée")));

        assertThat(securityAlertService.getFailureCount("ip_not_whitelisted")).isEqualTo(1.0);
        verify(paymentRepository, never()).findByCode(any());
    }

    @Test
    @DisplayName("Test : même event_id 2 fois → 1 seul traitement (idempotence webhook_events)")
    void webhook_sameEventIdTwice_processedOnlyOnce() throws Exception {
        Payment payment = buildPendingPayment(102L, "PAY-SEC002");
        when(paymentRepository.findByCode("PAY-SEC002")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(webhookEventRepository.existsByEventId(anyString())).thenAnswer(inv -> savedEventIds.contains(inv.getArgument(0)));
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(inv -> {
            WebhookEvent ev = inv.getArgument(0);
            savedEventIds.add(ev.getEventId());
            return ev;
        });

        Map<String, String> params = new LinkedHashMap<>();
        params.put("event_id", "EVT-DUP-2026");
        params.put("oid", "PAY-SEC002");
        params.put("amount", "750.00");
        params.put("Response", "Approved");
        params.put("ProcReturnCode", "00");
        params.put("TransId", "TX-DUP-01");
        String hash = hashService.generateInitiateHash(params, storeKey, "HMAC_SHA512");

        // 1er envoi
        mockMvc.perform(post("/api/payments/cmi/webhook")
                        .header("X-Forwarded-For", "196.200.160.10")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("event_id", "EVT-DUP-2026")
                        .param("oid", "PAY-SEC002")
                        .param("amount", "750.00")
                        .param("Response", "Approved")
                        .param("ProcReturnCode", "00")
                        .param("TransId", "TX-DUP-01")
                        .param("hash", hash))
                .andExpect(status().isOk())
                .andExpect(content().string("ACTION=POSTAUTH"));

        // 2ème envoi avec le même event_id
        mockMvc.perform(post("/api/payments/cmi/webhook")
                        .header("X-Forwarded-For", "196.200.160.10")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("event_id", "EVT-DUP-2026")
                        .param("oid", "PAY-SEC002")
                        .param("amount", "750.00")
                        .param("Response", "Approved")
                        .param("ProcReturnCode", "00")
                        .param("TransId", "TX-DUP-01")
                        .param("hash", hash))
                .andExpect(status().isOk())
                .andExpect(content().string("ACTION=POSTAUTH"));

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(webhookEventRepository, times(1)).save(any(WebhookEvent.class));
        verify(eventPublisher, times(1)).publishEvent(any(PaymentSucceededEvent.class));
    }

    @Test
    @DisplayName("Test : rate limit (> 10 req/min par IP) → 429 Too Many Requests + alerte Prometheus")
    void webhook_rateLimitExceeded_returns429TooManyRequests() throws Exception {
        Payment payment = buildPendingPayment(103L, "PAY-SEC003");
        when(paymentRepository.findByCode("PAY-SEC003")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(webhookEventRepository.existsByEventId(anyString())).thenAnswer(inv -> savedEventIds.contains(inv.getArgument(0)));
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(inv -> {
            WebhookEvent ev = inv.getArgument(0);
            savedEventIds.add(ev.getEventId());
            return ev;
        });

        Map<String, String> params = new LinkedHashMap<>();
        params.put("oid", "PAY-SEC003");
        params.put("amount", "750.00");
        params.put("Response", "Approved");
        params.put("ProcReturnCode", "00");
        params.put("TransId", "TX-RL-01");
        String hash = hashService.generateInitiateHash(params, storeKey, "HMAC_SHA512");

        // Les 10 premières requêtes de l'IP 41.137.10.20 passent (200 OK)
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post("/api/payments/cmi/webhook")
                            .header("X-Forwarded-For", "41.137.10.20")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("oid", "PAY-SEC003")
                            .param("amount", "750.00")
                            .param("Response", "Approved")
                            .param("ProcReturnCode", "00")
                            .param("TransId", "TX-RL-01")
                            .param("hash", hash))
                    .andExpect(status().isOk());
        }

        // La 11ème requête dans la même minute retourne 429 Too Many Requests
        mockMvc.perform(post("/api/payments/cmi/webhook")
                        .header("X-Forwarded-For", "41.137.10.20")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("oid", "PAY-SEC003")
                        .param("amount", "750.00")
                        .param("Response", "Approved")
                        .param("ProcReturnCode", "00")
                        .param("TransId", "TX-RL-01")
                        .param("hash", hash))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.detail", containsString("Trop de requêtes")));

        assertThat(securityAlertService.getFailureCount("rate_limit_exceeded")).isEqualTo(1.0);
    }

    private Payment buildPendingPayment(Long id, String code) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setCode(code);
        payment.setReservationId(10L);
        payment.setUserId(1L);
        payment.setMontant(new BigDecimal("750.00"));
        payment.setDevise("MAD");
        payment.setMethode(PaymentMethod.CMI);
        payment.setStatut(PaymentStatus.PENDING);
        payment.setReference(code);
        return payment;
    }
}
