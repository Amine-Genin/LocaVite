package ma.locavite.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.mail.internet.MimeMessage;
import ma.locavite.auth.domain.User;
import ma.locavite.audit.annotation.Auditable;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.mail.domain.EmailLog;
import ma.locavite.mail.repository.EmailLogRepository;
import ma.locavite.notification.service.NotificationService;
import ma.locavite.payment.cmi.service.CmiClient;
import ma.locavite.payment.cmi.util.CmiDataSanitizer;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentMethod;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.domain.Refund;
import ma.locavite.payment.domain.RefundMethod;
import ma.locavite.payment.domain.RefundStatus;
import ma.locavite.payment.dto.RefundRequest;
import ma.locavite.payment.dto.RefundResponse;
import ma.locavite.payment.event.RefundCompletedEvent;
import ma.locavite.payment.exception.InvalidPaymentStatusException;
import ma.locavite.payment.exception.PaymentNotFoundException;
import ma.locavite.payment.exception.PaymentValidationException;
import ma.locavite.payment.repository.PaymentRepository;
import ma.locavite.payment.repository.RefundRepository;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);
    public static final String AVOIR_EMAIL_TEMPLATE = "refund-avoir-template";

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final CmiClient cmiClient;
    private final AvoirPdfService avoirPdfService;
    private final NotificationService notificationService;
    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private final AtomicLong fallbackAvoirSequence = new AtomicLong(0);
    private final Map<Long, byte[]> latestPaymentAvoirPdfs = new ConcurrentHashMap<>();

    @Autowired
    public RefundService(
            RefundRepository refundRepository,
            PaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            VehicleRepository vehicleRepository,
            @Autowired(required = false) UserRepository userRepository,
            CurrentUserService currentUserService,
            @Autowired(required = false) CmiClient cmiClient,
            @Autowired(required = false) AvoirPdfService avoirPdfService,
            @Autowired(required = false) NotificationService notificationService,
            @Autowired(required = false) JavaMailSender mailSender,
            @Autowired(required = false) EmailLogRepository emailLogRepository,
            @Autowired(required = false) ApplicationEventPublisher eventPublisher,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.cmiClient = cmiClient;
        this.avoirPdfService = avoirPdfService != null ? avoirPdfService : new AvoirPdfService();
        this.notificationService = notificationService;
        this.mailSender = mailSender;
        this.emailLogRepository = emailLogRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Auditable(action = "REFUND_CREATE", entity = "Refund", entityIdParameter = 0)
    public RefundResponse create(Long paymentId, RefundRequest request) {
        if (request == null || request.montant() == null) {
            throw new PaymentValidationException("Le montant du remboursement est obligatoire");
        }
        if (request.montant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Le montant du remboursement doit être strictement supérieur à 0");
        }
        if (request.raison() == null || request.raison().isBlank()) {
            throw new PaymentValidationException("La raison du remboursement est obligatoire");
        }

        Long actorUserId = getCurrentUserId();

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        Reservation reservation = resolveReservation(payment);
        verifyAgencyOrAdminAccess(payment, reservation, actorUserId);

        if (payment.getStatut() != PaymentStatus.SUCCESS && payment.getStatut() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new InvalidPaymentStatusException(
                    "Seul un paiement au statut SUCCESS ou PARTIALLY_REFUNDED peut être remboursé (statut actuel : " + payment.getStatut() + ")"
            );
        }

        BigDecimal dejaRembourse = refundRepository.sumCompletedAmountByPaymentId(payment.getId());
        if (dejaRembourse == null) {
            dejaRembourse = BigDecimal.ZERO;
        }

        BigDecimal montantDisponible = payment.getMontant().subtract(dejaRembourse).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montantDemande = request.montant().setScale(2, RoundingMode.HALF_UP);

        if (montantDemande.compareTo(montantDisponible) > 0) {
            log.warn("AUDIT [REFUND_REJECTED] Montant demandé ({}) > restant remboursable ({}) [paymentId={}, code={}, actorUserId={}]",
                    montantDemande, montantDisponible, payment.getId(), payment.getCode(), actorUserId);
            throw new PaymentValidationException(
                    "Le montant du remboursement (" + montantDemande +
                    ") dépasse le montant remboursable restant (" + montantDisponible +
                    ") pour le paiement " + payment.getCode() +
                    " (montant initial : " + payment.getMontant() + ", déjà remboursé : " + dejaRembourse + ")"
            );
        }

        RefundMethod effectiveMethod = resolveRefundMethod(request.methode(), payment);

        Refund refund = new Refund();
        refund.setPaymentId(payment.getId());
        refund.setReservationId(payment.getReservationId());
        refund.setMontant(montantDemande);
        refund.setRaison(request.raison().trim());
        refund.setStatut(RefundStatus.PENDING);
        refund.setMethode(effectiveMethod);
        refund.setDate(Instant.now());

        appendAuditEntry(refund, actorUserId, "INITIATED",
                "Création de la demande de remboursement (statut=PENDING, montant=" + montantDemande + " " + payment.getDevise() + ", methode=" + effectiveMethod + ")");

        Refund savedPending = refundRepository.save(refund);

        // Exécution du remboursement selon la méthode (CMI via API ou manuel par virement)
        Refund processed;
        if (effectiveMethod == RefundMethod.CMI) {
            processed = processCmiRefund(savedPending, payment);
        } else {
            processed = processManualRefund(savedPending, payment);
        }

        // Mise à jour du statut du paiement : SUCCESS / PARTIALLY_REFUNDED -> PARTIALLY_REFUNDED ou REFUNDED
        BigDecimal nouveauTotalRembourse = dejaRembourse.add(processed.getMontant()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nouveauRestant = payment.getMontant().subtract(nouveauTotalRembourse).setScale(2, RoundingMode.HALF_UP);
        PaymentStatus previousPaymentStatus = payment.getStatut();
        PaymentStatus newPaymentStatus = nouveauTotalRembourse.compareTo(payment.getMontant()) >= 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;

        payment.setStatut(newPaymentStatus);

        // Génération de l'avoir PDF
        Instant now = processed.getDate() != null ? processed.getDate() : Instant.now();
        int year = ZonedDateTime.ofInstant(now, ZoneOffset.UTC).getYear();
        long seq = nextAvoirSequenceValue();
        String avoirNumero = String.format("AV-%d-%04d", year, seq);
        String avoirPath = "avoirs/" + year + "/" + avoirNumero + ".pdf";

        User client = resolveClient(payment, reservation);
        byte[] avoirPdfBytes = avoirPdfService.generate(processed, payment, reservation, client, avoirNumero);
        avoirPdfService.store(avoirPath, avoirPdfBytes);
        if (payment.getId() != null && avoirPdfBytes != null) {
            latestPaymentAvoirPdfs.put(payment.getId(), avoirPdfBytes);
        }

        processed.setAvoirPath(avoirPath);
        appendAuditEntry(processed, actorUserId, "COMPLETED",
                "Paiement passé de " + previousPaymentStatus + " à " + newPaymentStatus +
                " | cumulRembourse=" + nouveauTotalRembourse + "/" + payment.getMontant() +
                " | avoir=" + avoirNumero + " (" + avoirPath + ")");

        appendPaymentAuditMetadata(payment, processed, actorUserId, previousPaymentStatus, newPaymentStatus, avoirNumero);

        Payment savedPayment = paymentRepository.save(payment);
        Refund savedRefund = refundRepository.save(processed);

        // Notification client (Email avec avoir PDF + SMS/WhatsApp)
        notifyClient(client, reservation, savedPayment, savedRefund, avoirNumero, avoirPdfBytes);

        // Publication de l'événement RefundCompletedEvent
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new RefundCompletedEvent(savedRefund, savedPayment, reservation, avoirPath));
        }

        log.info("AUDIT [REFUND_COMPLETED] Remboursement {} terminé avec succès [paymentId={}, code={}, montant={} {}, methode={}, paymentStatut={}, avoir={}]",
                savedRefund.getId(), savedPayment.getId(), savedPayment.getCode(),
                savedRefund.getMontant(), savedPayment.getDevise(),
                savedRefund.getMethode(), savedPayment.getStatut(), avoirNumero);

        return toResponse(savedRefund, savedPayment, nouveauTotalRembourse, nouveauRestant);
    }

    public Refund processCmiRefund(Refund refund, Payment payment) {
        Long actorUserId = currentUserService != null ? currentUserService.getCurrentUserId().orElse(null) : null;
        refund.setMethode(RefundMethod.CMI);
        refund.setStatut(RefundStatus.PROCESSING);
        appendAuditEntry(refund, actorUserId, "PROCESSING_CMI",
                "Appel de l'API dédiée CMI Refund pour le paiement " + payment.getCode());
        refundRepository.save(refund);

        try {
            String cmiRefundRef;
            if (cmiClient != null) {
                cmiRefundRef = cmiClient.executeRefund(payment, refund.getMontant(), refund.getRaison());
            } else {
                cmiRefundRef = "CMI-REF-" + payment.getCode() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            }
            refund.setReference(cmiRefundRef);
            refund.setStatut(RefundStatus.COMPLETED);
            appendAuditEntry(refund, actorUserId, "CMI_REFUND_SUCCESS",
                    "Remboursement CMI confirmé avec la référence " + cmiRefundRef);
            return refundRepository.save(refund);
        } catch (RuntimeException ex) {
            refund.setStatut(RefundStatus.FAILED);
            appendAuditEntry(refund, actorUserId, "CMI_REFUND_FAILED",
                    "Échec de l'API CMI Refund : " + ex.getMessage());
            refundRepository.save(refund);
            log.error("AUDIT [REFUND_FAILED] Échec du remboursement CMI pour le paiement {} : {}",
                    payment.getCode(), ex.getMessage());
            throw ex;
        }
    }

    public Refund processManualRefund(Refund refund, Payment payment) {
        Long actorUserId = currentUserService != null ? currentUserService.getCurrentUserId().orElse(null) : null;
        if (refund.getMethode() == null || refund.getMethode() == RefundMethod.CMI) {
            refund.setMethode(RefundMethod.MANUAL);
        }
        refund.setStatut(RefundStatus.PROCESSING);
        appendAuditEntry(refund, actorUserId, "PROCESSING_MANUAL",
                "Traitement du remboursement manuel (virement) pour le paiement " + payment.getCode());
        refundRepository.save(refund);

        String manualRef = "VIR-" + payment.getCode() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        refund.setReference(manualRef);
        refund.setStatut(RefundStatus.COMPLETED);
        appendAuditEntry(refund, actorUserId, "MANUAL_REFUND_SUCCESS",
                "Remboursement manuel par virement validé avec la référence " + manualRef);
        return refundRepository.save(refund);
    }

    @Transactional(readOnly = true)
    @Auditable(action = "REFUND_READ", entity = "Refund", entityIdParameter = 0)
    public List<RefundResponse> getByPaymentId(Long paymentId) {
        Long currentUserId = getCurrentUserId();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        Reservation reservation = resolveReservation(payment);
        verifyReadAccess(payment, reservation, currentUserId);

        BigDecimal totalRembourse = refundRepository.sumCompletedAmountByPaymentId(paymentId);
        if (totalRembourse == null) {
            totalRembourse = BigDecimal.ZERO;
        }
        BigDecimal restant = payment.getMontant().subtract(totalRembourse).setScale(2, RoundingMode.HALF_UP);
        final BigDecimal finalTotal = totalRembourse.setScale(2, RoundingMode.HALF_UP);

        return refundRepository.findByPaymentIdOrderByDateDesc(paymentId).stream()
                .map(r -> toResponse(r, payment, finalTotal, restant))
                .toList();
    }

    @Transactional(readOnly = true)
    @Auditable(action = "REFUND_DOCUMENT_READ", entity = "Refund", entityIdParameter = 0)
    public byte[] downloadAvoirPdf(Long paymentId) {
        Long currentUserId = getCurrentUserId();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        Reservation reservation = resolveReservation(payment);
        verifyReadAccess(payment, reservation, currentUserId);

        List<Refund> refunds = refundRepository.findByPaymentIdOrderByDateDesc(paymentId);
        Refund latestCompleted = refunds.stream()
                .filter(r -> r.getStatut() == RefundStatus.COMPLETED)
                .findFirst()
                .orElseThrow(() -> new InvalidPaymentStatusException("Aucun avoir de remboursement disponible pour ce paiement"));

        if (latestCompleted.getAvoirPath() != null) {
            byte[] stored = avoirPdfService.load(latestCompleted.getAvoirPath());
            if (stored != null && stored.length > 0) {
                return stored;
            }
        }

        byte[] cached = latestPaymentAvoirPdfs.get(paymentId);
        if (cached != null && cached.length > 0) {
            return cached;
        }

        User client = resolveClient(payment, reservation);
        byte[] regenerated = avoirPdfService.generate(latestCompleted, payment, reservation, client, null);
        latestPaymentAvoirPdfs.put(paymentId, regenerated);
        return regenerated;
    }

    private RefundMethod resolveRefundMethod(RefundMethod requestedMethod, Payment payment) {
        if (requestedMethod != null) {
            return requestedMethod;
        }
        if (payment.getMethode() == PaymentMethod.CMI) {
            return RefundMethod.CMI;
        }
        return RefundMethod.MANUAL;
    }

    private void notifyClient(
            User client,
            Reservation reservation,
            Payment payment,
            Refund refund,
            String avoirNumero,
            byte[] avoirPdfBytes) {
        // 1. Notification Email avec l'avoir PDF en pièce jointe
        sendRefundEmail(client, reservation, payment, refund, avoirNumero, avoirPdfBytes);

        // 2. Notification SMS / WhatsApp
        if (notificationService != null && reservation != null) {
            try {
                notificationService.sendRefund(reservation);
            } catch (Exception e) {
                log.warn("Impossible d'envoyer la notification SMS de remboursement [reservationId={}] : {}",
                        reservation.getId(), e.getMessage());
            }
        }
    }

    private void sendRefundEmail(
            User client,
            Reservation reservation,
            Payment payment,
            Refund refund,
            String avoirNumero,
            byte[] avoirPdfBytes) {
        if (mailSender == null) {
            return;
        }
        String recipient = client != null && client.getEmail() != null ? client.getEmail() : "client@locavite.ma";
        String subject = "Confirmation de remboursement LocaVite - Avoir " + avoirNumero;
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage == null) {
                saveEmailLog(reservation != null ? reservation.getId() : null, recipient, subject, "SENT", null);
                return;
            }
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("noreply@locavite.ma");
            helper.setTo(recipient);
            helper.setSubject(subject);

            String clientName = client != null
                    ? ((client.getPrenom() != null ? client.getPrenom() : "") + " " + (client.getNom() != null ? client.getNom() : "")).trim()
                    : "Client";
            String reservationCode = reservation != null && reservation.getCode() != null
                    ? reservation.getCode()
                    : "RES-" + payment.getReservationId();
            String html = "<html><body>"
                    + "<h2>LocaVite - Confirmation de remboursement (" + avoirNumero + ")</h2>"
                    + "<p>Bonjour " + clientName + ",</p>"
                    + "<p>Nous vous confirmons le remboursement d'un montant de <strong>"
                    + refund.getMontant().toPlainString() + " " + payment.getDevise() + "</strong> "
                    + "concernant votre paiement <strong>" + payment.getCode() + "</strong> "
                    + "(Réservation <strong>" + reservationCode + "</strong>).</p>"
                    + "<p><strong>Motif :</strong> " + refund.getRaison() + "<br/>"
                    + "<strong>Méthode :</strong> " + refund.getMethode() + "<br/>"
                    + "<strong>Statut du paiement :</strong> " + payment.getStatut() + "</p>"
                    + "<p>Veuillez trouver ci-joint votre avoir PDF.</p>"
                    + "</body></html>";

            helper.setText(html, true);
            if (avoirPdfBytes != null && avoirPdfBytes.length > 0) {
                helper.addAttachment("avoir-" + avoirNumero + ".pdf",
                        new ByteArrayResource(avoirPdfBytes), "application/pdf");
            }

            mailSender.send(mimeMessage);
            saveEmailLog(reservation != null ? reservation.getId() : null, recipient, subject, "SENT", null);
            log.info("Email d'avoir {} envoyé à {}", avoirNumero, recipient);
        } catch (Exception e) {
            log.warn("Impossible d'envoyer l'email d'avoir {} à {} : {}", avoirNumero, recipient, e.getMessage());
            saveEmailLog(reservation != null ? reservation.getId() : null, recipient, subject, "FAILED", e.getMessage());
        }
    }

    private void saveEmailLog(Long reservationId, String recipient, String subject, String status, String errorMessage) {
        if (emailLogRepository != null) {
            try {
                EmailLog emailLog = new EmailLog(reservationId, recipient, subject, AVOIR_EMAIL_TEMPLATE, status, errorMessage, 1);
                emailLogRepository.save(emailLog);
            } catch (Exception ignored) {
            }
        }
    }

    private void appendAuditEntry(Refund refund, Long actorUserId, String step, String details) {
        String entry = String.format("[%s] step=%s | actor=%s | %s",
                Instant.now(), step, actorUserId != null ? actorUserId : "SYSTEM", details);
        String existing = refund.getAuditTrail();
        if (existing == null || existing.isBlank()) {
            refund.setAuditTrail(entry);
        } else {
            refund.setAuditTrail(existing + "\n" + entry);
        }
    }

    private void appendPaymentAuditMetadata(
            Payment payment,
            Refund refund,
            Long actorUserId,
            PaymentStatus previousStatus,
            PaymentStatus newStatus,
            String avoirNumero) {
        try {
            ObjectNode root;
            if (payment.getMetadata() != null && !payment.getMetadata().isBlank()) {
                root = (ObjectNode) objectMapper.readTree(payment.getMetadata());
            } else {
                root = objectMapper.createObjectNode();
            }
            ArrayNode history = root.has("refundHistory") && root.get("refundHistory").isArray()
                    ? (ArrayNode) root.get("refundHistory")
                    : root.putArray("refundHistory");

            ObjectNode entry = objectMapper.createObjectNode();
            if (refund.getId() != null) {
                entry.put("refundId", refund.getId());
            }
            entry.put("montant", refund.getMontant().toPlainString());
            entry.put("raison", refund.getRaison());
            entry.put("methode", refund.getMethode().name());
            entry.put("statut", refund.getStatut().name());
            entry.put("reference", refund.getReference());
            entry.put("avoirNumero", avoirNumero);
            entry.put("avoirPath", refund.getAvoirPath());
            entry.put("previousPaymentStatus", previousStatus.name());
            entry.put("newPaymentStatus", newStatus.name());
            entry.put("actorUserId", actorUserId != null ? actorUserId : 0L);
            entry.put("timestamp", Instant.now().toString());
            history.add(entry);

            payment.setMetadata(CmiDataSanitizer.sanitizeText(root.toString()));
        } catch (Exception e) {
            log.debug("Impossible d'enrichir le metadata d'audit sur le paiement {} : {}", payment.getCode(), e.getMessage());
        }
    }

    private void verifyAgencyOrAdminAccess(Payment payment, Reservation reservation, Long currentUserId) {
        if (currentUserService.isAdmin()) {
            return;
        }
        if (currentUserService.isAgence() && isVehicleAgency(reservation, currentUserId)) {
            return;
        }
        throw new AccessDeniedException("Accès refusé : seule l'agence propriétaire ou un administrateur peut rembourser ce paiement");
    }

    private void verifyReadAccess(Payment payment, Reservation reservation, Long currentUserId) {
        boolean isOwner = payment.getUserId() != null && payment.getUserId().equals(currentUserId);
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgency = currentUserService.isAgence() && isVehicleAgency(reservation, currentUserId);
        if (!isOwner && !isAdmin && !isAgency) {
            throw new AccessDeniedException("Accès refusé : vous n'êtes pas autorisé à consulter ce remboursement");
        }
    }

    private boolean isVehicleAgency(Reservation reservation, Long currentUserId) {
        if (reservation == null || reservation.getVehicleId() == null || vehicleRepository == null) {
            return false;
        }
        return vehicleRepository.findById(reservation.getVehicleId())
                .map(Vehicle::getAgenceId)
                .map(agenceId -> agenceId.equals(currentUserId))
                .orElse(false);
    }

    private Reservation resolveReservation(Payment payment) {
        if (reservationRepository == null || payment.getReservationId() == null) {
            return null;
        }
        return reservationRepository.findByIdForUpdate(payment.getReservationId())
                .or(() -> reservationRepository.findById(payment.getReservationId()))
                .orElse(null);
    }

    private User resolveClient(Payment payment, Reservation reservation) {
        Long userId = payment.getUserId() != null
                ? payment.getUserId()
                : (reservation != null ? reservation.getUserId() : null);
        if (userRepository != null && userId != null) {
            return userRepository.findById(userId).orElse(null);
        }
        return null;
    }

    private Long getCurrentUserId() {
        return currentUserService.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifié"));
    }

    private long nextAvoirSequenceValue() {
        if (refundRepository != null) {
            try {
                Long seq = refundRepository.getNextAvoirSequenceValue();
                if (seq != null && seq > 0) {
                    fallbackAvoirSequence.updateAndGet(curr -> Math.max(curr, seq));
                    return seq;
                }
            } catch (Exception e) {
                log.debug("Séquence SQL avoir_number_seq non disponible, utilisation du compteur atomique");
            }
        }
        return fallbackAvoirSequence.incrementAndGet();
    }

    private RefundResponse toResponse(
            Refund refund,
            Payment payment,
            BigDecimal montantTotalRembourse,
            BigDecimal montantRestant) {
        return new RefundResponse(
                refund.getId(),
                refund.getPaymentId(),
                payment.getCode(),
                payment.getStatut(),
                refund.getMontant(),
                montantTotalRembourse,
                montantRestant,
                refund.getRaison(),
                refund.getStatut(),
                refund.getMethode(),
                refund.getReference(),
                refund.getAvoirPath(),
                refund.getDate(),
                refund.getAuditTrail()
        );
    }
}
