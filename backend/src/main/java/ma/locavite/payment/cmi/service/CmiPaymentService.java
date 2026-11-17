package ma.locavite.payment.cmi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import ma.locavite.payment.cmi.config.CmiProperties;
import ma.locavite.audit.annotation.Auditable;
import ma.locavite.payment.cmi.domain.WebhookEvent;
import ma.locavite.payment.cmi.dto.CmiInitiateRequest;
import ma.locavite.payment.cmi.dto.CmiInitiateResponse;
import ma.locavite.payment.cmi.dto.CmiWebhookResponse;
import ma.locavite.payment.cmi.exception.CmiIpNotAllowedException;
import ma.locavite.payment.cmi.exception.CmiRateLimitExceededException;
import ma.locavite.payment.cmi.exception.CmiSignatureException;
import ma.locavite.payment.cmi.exception.CmiWebhookSignatureException;
import ma.locavite.payment.cmi.filter.CmiWebhookFilter;
import ma.locavite.payment.cmi.repository.WebhookEventRepository;
import ma.locavite.payment.cmi.util.CmiDataSanitizer;
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
import ma.locavite.reservation.exception.ReservationNotFoundException;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class CmiPaymentService {

    private static final Logger log = LoggerFactory.getLogger(CmiPaymentService.class);
    private static final String CODE_PREFIX = "PAY-";
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;
    private final CmiProperties cmiProperties;
    private final CmiHashService cmiHashService;
    private final CmiIpWhitelistService ipWhitelistService;
    private final CmiClient cmiClient;
    private final ObjectMapper objectMapper;
    private final WebhookEventRepository webhookEventRepository;
    private final CmiWebhookRateLimiter rateLimiter;
    private final CmiSecurityAlertService securityAlertService;
    private final Set<String> processedEventIdsFallback = ConcurrentHashMap.newKeySet();

    public CmiPaymentService(
            PaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            CurrentUserService currentUserService,
            ApplicationEventPublisher eventPublisher,
            CmiProperties cmiProperties,
            CmiHashService cmiHashService,
            CmiIpWhitelistService ipWhitelistService,
            CmiClient cmiClient,
            ObjectMapper objectMapper) {
        this(
                paymentRepository,
                reservationRepository,
                currentUserService,
                eventPublisher,
                cmiProperties,
                cmiHashService,
                ipWhitelistService,
                cmiClient,
                objectMapper,
                null,
                new CmiWebhookRateLimiter(cmiProperties),
                new CmiSecurityAlertService()
        );
    }

    @Autowired
    public CmiPaymentService(
            PaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            CurrentUserService currentUserService,
            ApplicationEventPublisher eventPublisher,
            CmiProperties cmiProperties,
            CmiHashService cmiHashService,
            CmiIpWhitelistService ipWhitelistService,
            CmiClient cmiClient,
            ObjectMapper objectMapper,
            @Nullable WebhookEventRepository webhookEventRepository,
            @Nullable CmiWebhookRateLimiter rateLimiter,
            @Nullable CmiSecurityAlertService securityAlertService) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
        this.cmiProperties = cmiProperties;
        this.cmiHashService = cmiHashService;
        this.ipWhitelistService = ipWhitelistService;
        this.cmiClient = cmiClient;
        this.objectMapper = objectMapper;
        this.webhookEventRepository = webhookEventRepository;
        this.rateLimiter = rateLimiter != null ? rateLimiter : new CmiWebhookRateLimiter(cmiProperties);
        this.securityAlertService = securityAlertService != null ? securityAlertService : new CmiSecurityAlertService();
    }

    @Auditable(action = "PAYMENT_CMI_INITIATE", entity = "Payment")
    public CmiInitiateResponse initiate(CmiInitiateRequest request, HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId();

        Reservation reservation = reservationRepository.findByIdForUpdate(request.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(request.reservationId()));

        if (!reservation.getUserId().equals(currentUserId) && !currentUserService.isAdmin()) {
            throw new AccessDeniedException("Accès refusé : vous n'êtes pas autorisé à payer pour cette réservation");
        }

        if (reservation.getStatut() == ReservationStatus.CANCELLED) {
            throw new PaymentValidationException("Impossible d'effectuer un paiement pour une réservation annulée");
        }

        BigDecimal totalSuccess = paymentRepository.sumSuccessAmountByReservationId(reservation.getId());
        if (totalSuccess == null) {
            totalSuccess = BigDecimal.ZERO;
        }

        BigDecimal prixRestant = reservation.getPrixTotal().subtract(totalSuccess);
        if (request.montant().compareTo(prixRestant) > 0) {
            throw new PaymentValidationException(
                    "Le montant du paiement (" + request.montant() +
                    ") dépasse le montant restant dû (" + prixRestant +
                    ") pour la réservation " + reservation.getCode()
            );
        }

        // Vérification de la disponibilité de la passerelle CMI (mode fallback)
        cmiClient.ensureGatewayAvailable();

        String paymentCode = generateUniqueCode();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(cmiProperties.getSessionTtlMinutes()));
        String clientIp = ipWhitelistService.getClientIp(httpRequest);

        String formattedAmount = request.montant().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String rnd = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Map<String, String> formData = new LinkedHashMap<>();
        formData.put("clientid", cmiProperties.getMerchantId());
        formData.put("storetype", cmiProperties.getStoreType());
        formData.put("trantype", cmiProperties.getTranType());
        formData.put("amount", formattedAmount);
        formData.put("currency", cmiProperties.getCurrency());
        formData.put("oid", paymentCode);
        formData.put("okUrl", cmiProperties.getCallbackUrl());
        formData.put("failUrl", cmiProperties.getCallbackUrl());
        formData.put("callbackUrl", cmiProperties.getWebhookUrl());
        formData.put("rnd", rnd);
        formData.put("lang", cmiProperties.getLang());
        formData.put("encoding", cmiProperties.getEncoding());

        String hash = cmiHashService.generateInitiateHash(
                formData,
                cmiProperties.getStoreKey(),
                cmiProperties.getSignatureAlgorithm()
        );
        formData.put("hash", hash);

        // Construction du metadata JSON complet
        ObjectNode metadataNode = objectMapper.createObjectNode();
        metadataNode.put("paymentMethod", "CMI");
        metadataNode.put("returnUrl", request.returnUrl());
        metadataNode.put("clientIp", clientIp);
        metadataNode.put("sessionExpiresAt", expiresAt.toString());
        metadataNode.put("sessionTtlMinutes", cmiProperties.getSessionTtlMinutes());

        ObjectNode cmiReqNode = objectMapper.createObjectNode();
        for (Map.Entry<String, String> entry : CmiDataSanitizer.maskSensitiveParams(formData).entrySet()) {
            cmiReqNode.put(entry.getKey(), entry.getValue());
        }
        metadataNode.set("cmiInitiateRequest", cmiReqNode);

        Payment payment = new Payment();
        payment.setCode(paymentCode);
        payment.setReservationId(reservation.getId());
        payment.setUserId(reservation.getUserId());
        payment.setMontant(request.montant());
        payment.setDevise("MAD");
        payment.setMethode(PaymentMethod.CMI);
        payment.setStatut(PaymentStatus.PENDING);
        payment.setReference(paymentCode);
        payment.setMetadata(CmiDataSanitizer.sanitizeText(metadataNode.toString()));

        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishEvent(new PaymentInitiatedEvent(saved));

        log.info("Session CMI initiée avec succès pour le paiement {} (réservation: {}, montant: {} MAD)",
                paymentCode, reservation.getId(), formattedAmount);

        String gatewayUrl = cmiProperties.getGatewayUrl();
        String redirectUrl = buildRedirectUrl(gatewayUrl, formData);

        return new CmiInitiateResponse(
                saved.getId(),
                saved.getCode(),
                saved.getMontant(),
                saved.getDevise(),
                gatewayUrl,
                redirectUrl,
                formData,
                expiresAt
        );
    }

    @Auditable(action = "PAYMENT_CMI_CALLBACK", entity = "Payment")
    public String handleCallback(Map<String, String> params, HttpServletRequest httpRequest) {
        log.info("Réception callback CMI avec paramètres : {}", CmiDataSanitizer.maskSensitiveParams(params));

        boolean validHash = cmiHashService.verifyHash(params, cmiProperties.getStoreKey());
        if (!validHash) {
            log.warn("Rejet du callback CMI : signature ou hash invalide.");
            throw new CmiSignatureException("Signature CMI invalide ou altérée");
        }

        String oid = extractOid(params);
        Payment payment = findPaymentByOid(oid);

        String returnUrl = extractReturnUrl(payment);
        boolean sessionExpired = isSessionExpired(payment);
        if (sessionExpired) {
            log.warn("Callback CMI pour le paiement {} reçu après expiration de la session (TTL 15 min dépassé)", payment.getCode());
            if (payment.getStatut().canTransitionTo(PaymentStatus.FAILED)) {
                payment.setStatut(PaymentStatus.FAILED);
                appendMetadata(payment, "callbackError", "Session expirée (TTL 15 min)");
                Payment saved = paymentRepository.save(payment);
                eventPublisher.publishEvent(new PaymentFailedEvent(saved));
            }
            return buildRedirectUrlWithParams(returnUrl, payment.getId(), payment.getCode(), "FAILED", payment.getReservationId(), "SESSION_EXPIRED");
        }

        String response = params.get("Response");
        String procReturnCode = params.get("ProcReturnCode");
        String transId = params.get("TransId");

        boolean isApproved = "00".equals(procReturnCode) || "Approved".equalsIgnoreCase(response);

        if (isApproved) {
            if (payment.getStatut() == PaymentStatus.PENDING || payment.getStatut() == PaymentStatus.PROCESSING) {
                payment.setStatut(PaymentStatus.SUCCESS);
                payment.setPaidAt(Instant.now());
                if (transId != null && !transId.isBlank()) {
                    payment.setTransactionId(transId);
                }
                appendMetadata(payment, "cmiCallbackResponse", CmiDataSanitizer.maskSensitiveParams(params));
                Payment saved = paymentRepository.save(payment);
                eventPublisher.publishEvent(new PaymentSucceededEvent(saved));
                log.info("Paiement CMI {} confirmé avec succès (TransId: {})", payment.getCode(), transId);
            }
            return buildRedirectUrlWithParams(returnUrl, payment.getId(), payment.getCode(), "SUCCESS", payment.getReservationId(), null);
        } else {
            if (payment.getStatut() == PaymentStatus.PENDING || payment.getStatut() == PaymentStatus.PROCESSING) {
                payment.setStatut(PaymentStatus.FAILED);
                String errMsg = params.getOrDefault("ErrMsg", "Échec de paiement CMI");
                appendMetadata(payment, "cmiCallbackResponse", CmiDataSanitizer.maskSensitiveParams(params));
                appendMetadata(payment, "failureReason", errMsg);
                Payment saved = paymentRepository.save(payment);
                eventPublisher.publishEvent(new PaymentFailedEvent(saved));
                log.warn("Paiement CMI {} échoué (Code retour: {}, Message: {})", payment.getCode(), procReturnCode, errMsg);
            }
            return buildRedirectUrlWithParams(returnUrl, payment.getId(), payment.getCode(), "FAILED", payment.getReservationId(), params.get("ErrMsg"));
        }
    }

    @Auditable(action = "PAYMENT_CMI_WEBHOOK", entity = "Payment")
    public CmiWebhookResponse handleWebhook(Map<String, String> params, HttpServletRequest httpRequest) {
        String clientIp = ipWhitelistService.getClientIp(httpRequest);
        boolean verifiedByFilter = httpRequest != null
                && Boolean.TRUE.equals(httpRequest.getAttribute(CmiWebhookFilter.ATTR_WEBHOOK_VERIFIED));

        Map<String, String> sanitizedParams = CmiDataSanitizer.maskSensitiveParams(params);
        if (!verifiedByFilter) {
            log.info("AUDIT_WEBHOOK_CMI ip={} headers={} payload={}",
                    clientIp, extractHeaders(httpRequest), sanitizedParams);

            if (!ipWhitelistService.isIpAllowed(clientIp)) {
                log.warn("Notification webhook CMI rejetée : IP {} non autorisée", clientIp);
                securityAlertService.triggerSecurityAlert(
                        "ip_not_whitelisted",
                        clientIp,
                        "IP non autorisée pour le webhook CMI : " + clientIp
                );
                throw new CmiIpNotAllowedException("IP non autorisée pour le webhook CMI : " + clientIp);
            }

            if (!rateLimiter.tryConsume(clientIp)) {
                log.warn("Notification webhook CMI rejetée : rate limit dépassé pour l'IP {}", clientIp);
                securityAlertService.triggerSecurityAlert(
                        "rate_limit_exceeded",
                        clientIp,
                        "Dépassement de la limite de " + cmiProperties.getWebhookRateLimitPerMinute() + " req/min pour l'IP " + clientIp
                );
                throw new CmiRateLimitExceededException(
                        "Trop de requêtes sur le webhook CMI depuis l'IP " + clientIp + " (limite : " + cmiProperties.getWebhookRateLimitPerMinute() + " req/min)"
                );
            }

            boolean validHash = cmiHashService.verifyHash(params, cmiProperties.getStoreKey());
            if (!validHash) {
                log.warn("Notification webhook CMI rejetée : signature HMAC-SHA512 invalide");
                securityAlertService.triggerSecurityAlert(
                        "invalid_signature",
                        clientIp,
                        "Signature HMAC-SHA512 invalide pour oid=" + params.getOrDefault("oid", "unknown")
                );
                throw new CmiWebhookSignatureException("Signature CMI invalide");
            }
            securityAlertService.recordWebhookRequest("accepted");
        }

        String oid = extractOid(params);
        Payment payment = findPaymentByOid(oid);
        String eventId = resolveEventId(params, httpRequest, oid);

        // Vérification d'idempotence via la table webhook_events (event_id unique)
        if (isDuplicateWebhookEvent(eventId)) {
            log.info("Webhook CMI idempotent : l'événement {} pour le paiement {} a déjà été traité", eventId, payment.getCode());
            return new CmiWebhookResponse("ACTION=POSTAUTH", true, payment.getId(), payment.getStatut());
        }

        // Idempotence métier sur le statut du paiement
        if (payment.getStatut() == PaymentStatus.SUCCESS || payment.getStatut() == PaymentStatus.FAILED) {
            recordWebhookEvent(eventId, sanitizedParams, clientIp);
            log.info("Webhook CMI idempotent : le paiement {} est déjà au statut {}", payment.getCode(), payment.getStatut());
            return new CmiWebhookResponse("ACTION=POSTAUTH", true, payment.getId(), payment.getStatut());
        }

        String response = params.get("Response");
        String procReturnCode = params.get("ProcReturnCode");
        String transId = params.get("TransId");

        boolean isApproved = "00".equals(procReturnCode) || "Approved".equalsIgnoreCase(response);

        if (isApproved) {
            payment.setStatut(PaymentStatus.SUCCESS);
            payment.setPaidAt(Instant.now());
            if (transId != null && !transId.isBlank()) {
                payment.setTransactionId(transId);
            }
            appendMetadata(payment, "cmiWebhookResponse", sanitizedParams);
            appendMetadata(payment, "webhookEventId", eventId);
            Payment saved = paymentRepository.save(payment);
            recordWebhookEvent(eventId, sanitizedParams, clientIp);
            eventPublisher.publishEvent(new PaymentSucceededEvent(saved));
            log.info("Paiement CMI {} confirmé via webhook (TransId: {}, eventId: {})", payment.getCode(), transId, eventId);
            return new CmiWebhookResponse("ACTION=POSTAUTH", false, saved.getId(), saved.getStatut());
        } else {
            payment.setStatut(PaymentStatus.FAILED);
            appendMetadata(payment, "cmiWebhookResponse", sanitizedParams);
            appendMetadata(payment, "webhookEventId", eventId);
            appendMetadata(payment, "failureReason", params.getOrDefault("ErrMsg", "Échec CMI via notification serveur"));
            Payment saved = paymentRepository.save(payment);
            recordWebhookEvent(eventId, sanitizedParams, clientIp);
            eventPublisher.publishEvent(new PaymentFailedEvent(saved));
            log.warn("Paiement CMI {} échoué via webhook (eventId: {})", payment.getCode(), eventId);
            return new CmiWebhookResponse("ACTION=POSTAUTH", false, saved.getId(), saved.getStatut());
        }
    }

    private String resolveEventId(Map<String, String> params, HttpServletRequest request, String oid) {
        if (params != null) {
            for (String key : List.of("event_id", "eventId", "EVENT_ID", "EventId")) {
                String val = params.get(key);
                if (val != null && !val.isBlank()) {
                    return val.trim();
                }
            }
        }
        if (request != null) {
            String headerVal = request.getHeader("X-CMI-Event-Id");
            if (headerVal != null && !headerVal.isBlank()) {
                return headerVal.trim();
            }
        }
        String transId = params != null ? params.getOrDefault("TransId", "NO_TX") : "NO_TX";
        String procCode = params != null ? params.getOrDefault("ProcReturnCode", "NA") : "NA";
        return "CMI:" + (oid != null ? oid : "UNKNOWN") + ":" + transId + ":" + procCode;
    }

    private boolean isDuplicateWebhookEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        if (webhookEventRepository != null) {
            return webhookEventRepository.existsByEventId(eventId);
        }
        return processedEventIdsFallback.contains(eventId);
    }

    private void recordWebhookEvent(String eventId, Map<String, String> sanitizedParams, String clientIp) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        processedEventIdsFallback.add(eventId);
        if (webhookEventRepository != null && !webhookEventRepository.existsByEventId(eventId)) {
            Instant now = Instant.now();
            WebhookEvent event = new WebhookEvent();
            event.setEventId(eventId);
            event.setSource("CMI");
            event.setIp(clientIp != null && !clientIp.isBlank() ? clientIp : "unknown");
            try {
                event.setPayload(objectMapper.writeValueAsString(sanitizedParams));
            } catch (Exception e) {
                event.setPayload(String.valueOf(sanitizedParams));
            }
            event.setReceivedAt(now);
            event.setProcessedAt(now);
            webhookEventRepository.save(event);
        }
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        if (request == null) {
            return Collections.emptyMap();
        }
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("authorization") || lower.contains("cookie") || lower.contains("secret")) {
                    headers.put(name, "******");
                } else {
                    headers.put(name, request.getHeader(name));
                }
            }
        }
        return headers;
    }

    private Long getCurrentUserId() {
        return currentUserService.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifié"));
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_PREFIX);
            for (int i = 0; i < 6; i++) {
                sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
            }
            code = sb.toString();
        } while (paymentRepository.existsByCode(code));
        return code;
    }

    private Payment findPaymentByOid(String oid) {
        if (oid == null || oid.isBlank()) {
            throw new PaymentNotFoundException("Identifiant de paiement CMI (oid) manquant");
        }
        return paymentRepository.findByCode(oid)
                .or(() -> paymentRepository.findByReference(oid))
                .or(() -> {
                    try {
                        return paymentRepository.findById(Long.parseLong(oid));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                })
                .orElseThrow(() -> new PaymentNotFoundException("Paiement non trouvé pour l'identifiant CMI oid : " + oid));
    }

    private String extractOid(Map<String, String> params) {
        if (params.containsKey("oid")) {
            return params.get("oid");
        }
        if (params.containsKey("OID")) {
            return params.get("OID");
        }
        if (params.containsKey("ReturnOid")) {
            return params.get("ReturnOid");
        }
        return null;
    }

    private boolean isSessionExpired(Payment payment) {
        if (payment.getMetadata() == null || payment.getMetadata().isBlank()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(payment.getMetadata());
            if (root.has("sessionExpiresAt")) {
                Instant expiresAt = Instant.parse(root.get("sessionExpiresAt").asText());
                return Instant.now().isAfter(expiresAt);
            }
        } catch (Exception e) {
            log.debug("Impossible d'analyser la date d'expiration de session dans les métadonnées: {}", e.getMessage());
        }
        return false;
    }

    private String extractReturnUrl(Payment payment) {
        if (payment.getMetadata() != null && !payment.getMetadata().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(payment.getMetadata());
                if (root.has("returnUrl") && !root.get("returnUrl").asText().isBlank()) {
                    return root.get("returnUrl").asText();
                }
            } catch (Exception e) {
                log.debug("Erreur lecture returnUrl dans metadata: {}", e.getMessage());
            }
        }
        return "/reservations/" + payment.getReservationId() + "/confirmation";
    }

    private void appendMetadata(Payment payment, String key, Object value) {
        try {
            ObjectNode root;
            if (payment.getMetadata() != null && !payment.getMetadata().isBlank()) {
                root = (ObjectNode) objectMapper.readTree(payment.getMetadata());
            } else {
                root = objectMapper.createObjectNode();
            }
            if (value instanceof String strVal) {
                root.put(key, strVal);
            } else {
                root.set(key, objectMapper.valueToTree(value));
            }
            payment.setMetadata(CmiDataSanitizer.sanitizeText(root.toString()));
        } catch (Exception e) {
            log.warn("Impossible d'enrichir les métadonnées du paiement {}: {}", payment.getCode(), e.getMessage());
        }
    }

    private String buildRedirectUrl(String baseUrl, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(baseUrl);
        boolean first = !baseUrl.contains("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null) continue;
            sb.append(first ? "?" : "&");
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        return sb.toString();
    }

    private String buildRedirectUrlWithParams(String baseReturnUrl, Long paymentId, String code, String status, Long reservationId, String error) {
        StringBuilder sb = new StringBuilder(baseReturnUrl);
        sb.append(baseReturnUrl.contains("?") ? "&" : "?");
        sb.append("paymentId=").append(paymentId);
        sb.append("&code=").append(URLEncoder.encode(code, StandardCharsets.UTF_8));
        sb.append("&status=").append(status);
        if (reservationId != null) {
            sb.append("&reservationId=").append(reservationId);
        }
        if (error != null && !error.isBlank()) {
            sb.append("&error=").append(URLEncoder.encode(error, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
