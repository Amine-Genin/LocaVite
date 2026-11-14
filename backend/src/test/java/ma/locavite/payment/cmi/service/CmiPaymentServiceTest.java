package ma.locavite.payment.cmi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import ma.locavite.payment.cmi.config.CmiProperties;
import ma.locavite.payment.cmi.domain.WebhookEvent;
import ma.locavite.payment.cmi.dto.CmiInitiateRequest;
import ma.locavite.payment.cmi.dto.CmiInitiateResponse;
import ma.locavite.payment.cmi.dto.CmiWebhookResponse;
import ma.locavite.payment.cmi.exception.CmiIpNotAllowedException;
import ma.locavite.payment.cmi.exception.CmiRateLimitExceededException;
import ma.locavite.payment.cmi.exception.CmiSignatureException;
import ma.locavite.payment.cmi.exception.CmiUnavailableException;
import ma.locavite.payment.cmi.exception.CmiWebhookSignatureException;
import ma.locavite.payment.cmi.repository.WebhookEventRepository;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentMethod;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.event.PaymentFailedEvent;
import ma.locavite.payment.event.PaymentInitiatedEvent;
import ma.locavite.payment.event.PaymentSucceededEvent;
import ma.locavite.payment.exception.PaymentNotFoundException;
import ma.locavite.payment.exception.PaymentValidationException;
import ma.locavite.payment.repository.PaymentRepository;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CmiPaymentServiceTest {

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
    private HttpServletRequest httpRequest;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CmiHashService hashService = new CmiHashService();
    private CmiProperties properties;
    private CmiIpWhitelistService ipWhitelistService;
    private CmiPaymentService paymentService;

    private Reservation sampleReservation;
    private final String secretKey = "test_cmi_secret_key_123456";

    @BeforeEach
    void setUp() {
        properties = new CmiProperties();
        properties.setMerchantId("merchant_100");
        properties.setSecretKey(secretKey);
        properties.setStoreKey(secretKey);
        properties.setGatewayUrl("https://testpayment.cmi.co.ma/fim/est3Dgate");
        properties.setCallbackUrl("http://localhost:8081/api/payments/cmi/callback");
        properties.setWebhookUrl("http://localhost:8081/api/payments/cmi/webhook");
        properties.setSessionTtlMinutes(15);
        properties.setIpWhitelist(List.of("127.0.0.1", "::1", "196.200.160.0/24"));

        ipWhitelistService = new CmiIpWhitelistService(properties);

        paymentService = new CmiPaymentService(
                paymentRepository,
                reservationRepository,
                currentUserService,
                eventPublisher,
                properties,
                hashService,
                ipWhitelistService,
                cmiClient,
                objectMapper
        );

        sampleReservation = new Reservation();
        sampleReservation.setId(10L);
        sampleReservation.setCode("RES-1001");
        sampleReservation.setUserId(1L);
        sampleReservation.setPrixTotal(new BigDecimal("1000.00"));
        sampleReservation.setStatut(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("Test : initiation paiement → URL CMI générée avec signature HMAC-SHA512 et session 15 min")
    void initiate_validRequest_generatesCmiSessionAndRedirectUrl() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(1L));
        when(reservationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleReservation));
        when(paymentRepository.sumSuccessAmountByReservationId(10L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(42L);
            return p;
        });
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        CmiInitiateRequest request = new CmiInitiateRequest(
                10L,
                new BigDecimal("400.00"),
                "https://locavite.ma/reservations/10/confirmation"
        );

        CmiInitiateResponse response = paymentService.initiate(request, httpRequest);

        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(42L);
        assertThat(response.paymentCode()).startsWith("PAY-");
        assertThat(response.gatewayUrl()).isEqualTo("https://testpayment.cmi.co.ma/fim/est3Dgate");
        assertThat(response.redirectUrl()).contains("https://testpayment.cmi.co.ma/fim/est3Dgate");
        assertThat(response.formData()).containsKey("hash");
        assertThat(response.formData().get("clientid")).isEqualTo("merchant_100");
        assertThat(response.formData().get("amount")).isEqualTo("400.00");
        assertThat(response.formData().get("currency")).isEqualTo("504");
        assertThat(response.expiresAt()).isAfter(Instant.now());

        // Vérification de la signature générée dans le formulaire CMI
        boolean hashValid = hashService.verifyHash(response.formData(), secretKey);
        assertThat(hashValid).isTrue();

        // Vérification de la persistance du paiement PENDING
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getMethode()).isEqualTo(PaymentMethod.CMI);
        assertThat(saved.getStatut()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getMontant()).isEqualByComparingTo("400.00");
        assertThat(saved.getMetadata()).contains("sessionExpiresAt");

        verify(eventPublisher).publishEvent(any(PaymentInitiatedEvent.class));
    }

    @Test
    @DisplayName("Test : initiation paiement montant supérieur au restant dû → 400 Bad Request")
    void initiate_montantExceedsRemaining_throwsValidationException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(1L));
        when(reservationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleReservation));
        when(paymentRepository.sumSuccessAmountByReservationId(10L)).thenReturn(new BigDecimal("800.00"));

        // Reste 200.00, tentative de payer 300.00
        CmiInitiateRequest request = new CmiInitiateRequest(
                10L,
                new BigDecimal("300.00"),
                "https://locavite.ma/confirmation"
        );

        assertThatThrownBy(() -> paymentService.initiate(request, httpRequest))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessageContaining("dépasse le montant restant dû");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test : mode fallback si CMI indisponible → CmiUnavailableException")
    void initiate_cmiUnavailable_throwsCmiUnavailableException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(1L));
        when(reservationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleReservation));
        when(paymentRepository.sumSuccessAmountByReservationId(10L)).thenReturn(BigDecimal.ZERO);
        doThrow(new CmiUnavailableException("Passerelle CMI indisponible")).when(cmiClient).ensureGatewayAvailable();

        CmiInitiateRequest request = new CmiInitiateRequest(
                10L,
                new BigDecimal("200.00"),
                "https://locavite.ma/confirmation"
        );

        assertThatThrownBy(() -> paymentService.initiate(request, httpRequest))
                .isInstanceOf(CmiUnavailableException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test : callback avec hash valide et paiement approuvé → statut SUCCESS")
    void callback_validHashApproved_updatesPaymentToSuccess() {
        Payment payment = new Payment();
        payment.setId(42L);
        payment.setCode("PAY-CMI001");
        payment.setReservationId(10L);
        payment.setMontant(new BigDecimal("400.00"));
        payment.setStatut(PaymentStatus.PENDING);
        payment.setMetadata("{\"returnUrl\":\"https://locavite.ma/confirmation\",\"sessionExpiresAt\":\"" + Instant.now().plus(Duration.ofMinutes(10)) + "\"}");

        when(paymentRepository.findByCode("PAY-CMI001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, String> params = new HashMap<>();
        params.put("oid", "PAY-CMI001");
        params.put("Response", "Approved");
        params.put("ProcReturnCode", "00");
        params.put("TransId", "TXN-CMI-9988");
        params.put("amount", "400.00");
        String hash = hashService.generateInitiateHash(params, secretKey, "HMAC_SHA512");
        params.put("hash", hash);

        String redirectUrl = paymentService.handleCallback(params, httpRequest);

        assertThat(payment.getStatut()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionId()).isEqualTo("TXN-CMI-9988");
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(redirectUrl).contains("https://locavite.ma/confirmation")
                .contains("status=SUCCESS")
                .contains("paymentId=42");

        verify(eventPublisher).publishEvent(any(PaymentSucceededEvent.class));
    }

    @Test
    @DisplayName("Test : callback avec hash invalide → CmiSignatureException (400)")
    void callback_invalidHash_throwsCmiSignatureException() {
        Map<String, String> params = new HashMap<>();
        params.put("oid", "PAY-CMI001");
        params.put("Response", "Approved");
        params.put("ProcReturnCode", "00");
        params.put("hash", "CORRUPTED_HASH_XYZ==");

        assertThatThrownBy(() -> paymentService.handleCallback(params, httpRequest))
                .isInstanceOf(CmiSignatureException.class)
                .hasMessageContaining("Signature CMI invalide");

        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(PaymentSucceededEvent.class));
    }

    @Test
    @DisplayName("Test : callback pour paiement échoué (failUrl) → statut FAILED")
    void callback_declinedPayment_updatesPaymentToFailed() {
        Payment payment = new Payment();
        payment.setId(42L);
        payment.setCode("PAY-CMI001");
        payment.setReservationId(10L);
        payment.setMontant(new BigDecimal("400.00"));
        payment.setStatut(PaymentStatus.PENDING);
        payment.setMetadata("{\"returnUrl\":\"https://locavite.ma/confirmation\",\"sessionExpiresAt\":\"" + Instant.now().plus(Duration.ofMinutes(10)) + "\"}");

        when(paymentRepository.findByCode("PAY-CMI001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, String> params = new HashMap<>();
        params.put("oid", "PAY-CMI001");
        params.put("Response", "Declined");
        params.put("ProcReturnCode", "05");
        params.put("ErrMsg", "Solde insuffisant");
        params.put("amount", "400.00");
        String hash = hashService.generateInitiateHash(params, secretKey, "HMAC_SHA512");
        params.put("hash", hash);

        String redirectUrl = paymentService.handleCallback(params, httpRequest);

        assertThat(payment.getStatut()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getMetadata()).contains("Solde insuffisant");
        assertThat(redirectUrl).contains("status=FAILED");

        verify(eventPublisher).publishEvent(any(PaymentFailedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(PaymentSucceededEvent.class));
    }

    @Test
    @DisplayName("Test : callback avec session expirée (> 15 min TTL) → statut FAILED")
    void callback_expiredSession_marksPaymentFailed() {
        Payment payment = new Payment();
        payment.setId(42L);
        payment.setCode("PAY-CMI001");
        payment.setReservationId(10L);
        payment.setMontant(new BigDecimal("400.00"));
        payment.setStatut(PaymentStatus.PENDING);
        // Expiré il y a 5 minutes
        payment.setMetadata("{\"returnUrl\":\"https://locavite.ma/confirmation\",\"sessionExpiresAt\":\"" + Instant.now().minus(Duration.ofMinutes(5)) + "\"}");

        when(paymentRepository.findByCode("PAY-CMI001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, String> params = new HashMap<>();
        params.put("oid", "PAY-CMI001");
        params.put("Response", "Approved");
        params.put("ProcReturnCode", "00");
        params.put("amount", "400.00");
        String hash = hashService.generateInitiateHash(params, secretKey, "HMAC_SHA512");
        params.put("hash", hash);

        String redirectUrl = paymentService.handleCallback(params, httpRequest);

        assertThat(payment.getStatut()).isEqualTo(PaymentStatus.FAILED);
        assertThat(redirectUrl).contains("SESSION_EXPIRED");
        verify(eventPublisher).publishEvent(any(PaymentFailedEvent.class));
    }

    @Test
    @DisplayName("Test : webhook notification serveur-à-serveur idempotent (2 fois même ID → 1 seul traitement)")
    void webhook_idempotent_duplicateProcessedOnce() {
        Payment payment = new Payment();
        payment.setId(55L);
        payment.setCode("PAY-WB001");
        payment.setReservationId(10L);
        payment.setMontant(new BigDecimal("600.00"));
        payment.setStatut(PaymentStatus.PENDING);

        when(paymentRepository.findByCode("PAY-WB001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            return p;
        });
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        Map<String, String> params = new HashMap<>();
        params.put("oid", "PAY-WB001");
        params.put("Response", "Approved");
        params.put("ProcReturnCode", "00");
        params.put("TransId", "TXN-WB-77");
        params.put("amount", "600.00");
        String hash = hashService.generateInitiateHash(params, secretKey, "HMAC_SHA512");
        params.put("hash", hash);

        // Premier appel au webhook
        CmiWebhookResponse response1 = paymentService.handleWebhook(params, httpRequest);
        assertThat(response1.action()).isEqualTo("ACTION=POSTAUTH");
        assertThat(response1.duplicate()).isFalse();
        assertThat(payment.getStatut()).isEqualTo(PaymentStatus.SUCCESS);
        verify(eventPublisher, times(1)).publishEvent(any(PaymentSucceededEvent.class));

        // Deuxième appel avec les mêmes données (simulation retry ou doublon CMI)
        CmiWebhookResponse response2 = paymentService.handleWebhook(params, httpRequest);
        assertThat(response2.action()).isEqualTo("ACTION=POSTAUTH");
        assertThat(response2.duplicate()).isTrue();
        // Vérification qu'aucun 2ème événement n'a été publié
        verify(eventPublisher, times(1)).publishEvent(any(PaymentSucceededEvent.class));
    }

    @Test
    @DisplayName("Test : webhook avec IP non autorisée → rejet 403 (CmiIpNotAllowedException)")
    void webhook_unauthorizedIp_throwsCmiIpNotAllowedException() {
        when(httpRequest.getRemoteAddr()).thenReturn("198.51.100.25"); // IP publique arbitraire non whitelistée

        Map<String, String> params = new HashMap<>();
        params.put("oid", "PAY-WB001");

        assertThatThrownBy(() -> paymentService.handleWebhook(params, httpRequest))
                .isInstanceOf(CmiIpNotAllowedException.class)
                .hasMessageContaining("IP non autorisée");

        verify(paymentRepository, never()).findByCode(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Test : webhook avec signature invalide → CmiWebhookSignatureException (401)")
    void webhook_invalidSignature_throwsCmiSignatureException() {
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        Map<String, String> params = new HashMap<>();
        params.put("oid", "PAY-WB001");
        params.put("Response", "Approved");
        params.put("ProcReturnCode", "00");
        params.put("hash", "BAD_SIG");

        assertThatThrownBy(() -> paymentService.handleWebhook(params, httpRequest))
                .isInstanceOf(CmiWebhookSignatureException.class);

        verify(paymentRepository, never()).findByCode(any());
    }

    @Test
    @DisplayName("Test : même event_id 2 fois via table webhook_events → 1 seul traitement")
    void webhook_sameEventIdTwice_persistedInWebhookEventsAndProcessedOnce() {
        WebhookEventRepository webhookEventRepository = mock(WebhookEventRepository.class);
        CmiWebhookRateLimiter rateLimiter = new CmiWebhookRateLimiter(properties);
        CmiSecurityAlertService alertService = new CmiSecurityAlertService();

        CmiPaymentService serviceWithRepo = new CmiPaymentService(
                paymentRepository,
                reservationRepository,
                currentUserService,
                eventPublisher,
                properties,
                hashService,
                ipWhitelistService,
                cmiClient,
                objectMapper,
                webhookEventRepository,
                rateLimiter,
                alertService
        );

        Payment payment = new Payment();
        payment.setId(55L);
        payment.setCode("PAY-WB002");
        payment.setReservationId(10L);
        payment.setMontant(new BigDecimal("600.00"));
        payment.setStatut(PaymentStatus.PENDING);

        when(paymentRepository.findByCode("PAY-WB002")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(httpRequest.getRemoteAddr()).thenReturn("196.200.160.15");

        // 1er appel : event_id non encore présent en base, puis présent au 2ème appel
        when(webhookEventRepository.existsByEventId("EVT-CMI-9001"))
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);

        Map<String, String> params = new HashMap<>();
        params.put("event_id", "EVT-CMI-9001");
        params.put("oid", "PAY-WB002");
        params.put("Response", "Approved");
        params.put("ProcReturnCode", "00");
        params.put("TransId", "TXN-WB-88");
        params.put("amount", "600.00");
        params.put("hash", hashService.generateInitiateHash(params, secretKey, "HMAC_SHA512"));

        CmiWebhookResponse first = serviceWithRepo.handleWebhook(params, httpRequest);
        assertThat(first.action()).isEqualTo("ACTION=POSTAUTH");
        assertThat(first.duplicate()).isFalse();

        ArgumentCaptor<WebhookEvent> eventCaptor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookEventRepository, times(1)).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo("EVT-CMI-9001");
        assertThat(eventCaptor.getValue().getSource()).isEqualTo("CMI");
        assertThat(eventCaptor.getValue().getIp()).isEqualTo("196.200.160.15");
        assertThat(eventCaptor.getValue().getReceivedAt()).isNotNull();
        assertThat(eventCaptor.getValue().getProcessedAt()).isNotNull();

        // 2ème appel avec le même event_id
        CmiWebhookResponse second = serviceWithRepo.handleWebhook(params, httpRequest);
        assertThat(second.action()).isEqualTo("ACTION=POSTAUTH");
        assertThat(second.duplicate()).isTrue();
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(eventPublisher, times(1)).publishEvent(any(PaymentSucceededEvent.class));
    }

    @Test
    @DisplayName("Test : rate limit (> 10 req/min par IP) → CmiRateLimitExceededException (429)")
    void webhook_rateLimitExceeded_throwsCmiRateLimitExceededException() {
        when(httpRequest.getRemoteAddr()).thenReturn("196.200.160.42");

        Payment payment = new Payment();
        payment.setId(56L);
        payment.setCode("PAY-WB003");
        payment.setReservationId(10L);
        payment.setMontant(new BigDecimal("600.00"));
        payment.setStatut(PaymentStatus.PENDING);
        when(paymentRepository.findByCode("PAY-WB003")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, String> params = new HashMap<>();
        params.put("oid", "PAY-WB003");
        params.put("Response", "Approved");
        params.put("ProcReturnCode", "00");
        params.put("amount", "600.00");
        params.put("hash", hashService.generateInitiateHash(params, secretKey, "HMAC_SHA512"));

        // 10 requêtes autorisées dans la minute
        for (int i = 0; i < 10; i++) {
            CmiWebhookResponse resp = paymentService.handleWebhook(params, httpRequest);
            assertThat(resp.action()).isEqualTo("ACTION=POSTAUTH");
        }

        // La 11ème requête de la même IP dans la même minute doit être bloquée (429)
        assertThatThrownBy(() -> paymentService.handleWebhook(params, httpRequest))
                .isInstanceOf(CmiRateLimitExceededException.class)
                .hasMessageContaining("Trop de requêtes");
    }
}
