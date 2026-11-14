package ma.locavite.payment.service;

import ma.locavite.notification.service.NotificationService;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentMethod;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.dto.CashPaymentRequest;
import ma.locavite.payment.dto.PaymentResponse;
import ma.locavite.payment.event.PaymentInitiatedEvent;
import ma.locavite.payment.event.PaymentSucceededEvent;
import ma.locavite.payment.exception.InvalidPaymentStatusException;
import ma.locavite.payment.exception.PaymentNotFoundException;
import ma.locavite.payment.exception.PaymentValidationException;
import ma.locavite.payment.mapper.PaymentMapper;
import ma.locavite.payment.repository.PaymentRepository;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.exception.ReservationNotFoundException;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class CashPaymentService {

    private static final Logger log = LoggerFactory.getLogger(CashPaymentService.class);
    private static final String CODE_PREFIX = "PAY-";
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final PaymentMapper paymentMapper;
    private final CurrentUserService currentUserService;
    private final CashReceiptPdfService cashReceiptPdfService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<Long, byte[]> generatedReceipts = new ConcurrentHashMap<>();

    @Autowired
    public CashPaymentService(
            PaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            VehicleRepository vehicleRepository,
            PaymentMapper paymentMapper,
            CurrentUserService currentUserService,
            CashReceiptPdfService cashReceiptPdfService,
            NotificationService notificationService,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.paymentMapper = paymentMapper;
        this.currentUserService = currentUserService;
        this.cashReceiptPdfService = cashReceiptPdfService;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
    }

    public PaymentResponse createCashPayment(CashPaymentRequest request) {
        if (request == null || request.reservationId() == null) {
            throw new PaymentValidationException("L'identifiant de la réservation est obligatoire");
        }
        validateCashMethod(request.methode());

        Long currentUserId = getCurrentUserId();

        Reservation reservation = reservationRepository.findByIdForUpdate(request.reservationId())
                .or(() -> reservationRepository.findById(request.reservationId()))
                .orElseThrow(() -> new ReservationNotFoundException(request.reservationId()));

        if (!reservation.getUserId().equals(currentUserId) && !currentUserService.isAdmin()) {
            throw new AccessDeniedException("Accès refusé : vous n'êtes pas autorisé à créer un paiement pour cette réservation");
        }

        if (reservation.getStatut() == ReservationStatus.CANCELLED) {
            throw new PaymentValidationException("Impossible d'effectuer un paiement pour une réservation annulée");
        }

        BigDecimal totalSuccess = paymentRepository.sumSuccessAmountByReservationId(reservation.getId());
        if (totalSuccess == null) {
            totalSuccess = BigDecimal.ZERO;
        }

        BigDecimal prixRestant = reservation.getPrixTotal().subtract(totalSuccess);
        if (prixRestant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException(
                    "Aucun montant restant dû pour la réservation " + reservation.getCode()
            );
        }

        Payment payment = new Payment();
        payment.setCode(generateUniqueCode());
        payment.setReservationId(reservation.getId());
        payment.setUserId(reservation.getUserId());
        payment.setMontant(prixRestant);
        payment.setDevise("MAD");
        payment.setMethode(request.methode());
        payment.setStatut(PaymentStatus.PENDING);

        Payment saved = paymentRepository.save(payment);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new PaymentInitiatedEvent(saved));
        }

        return paymentMapper.toResponse(saved);
    }

    public PaymentResponse confirmCash(Long paymentId) {
        Long currentUserId = getCurrentUserId();

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        Reservation reservation = reservationRepository.findByIdForUpdate(payment.getReservationId())
                .or(() -> reservationRepository.findById(payment.getReservationId()))
                .orElseThrow(() -> new ReservationNotFoundException(payment.getReservationId()));

        verifyAgencyOrAdminAccess(reservation, currentUserId);
        validateCashMethod(payment.getMethode());

        if (payment.getStatut() != PaymentStatus.PENDING || !payment.getStatut().canTransitionTo(PaymentStatus.SUCCESS)) {
            throw new InvalidPaymentStatusException(
                    "Impossible de confirmer un paiement en espèces avec le statut : " + payment.getStatut()
            );
        }

        BigDecimal totalSuccess = paymentRepository.sumSuccessAmountByReservationId(reservation.getId());
        if (totalSuccess == null) {
            totalSuccess = BigDecimal.ZERO;
        }

        BigDecimal prixRestant = reservation.getPrixTotal().subtract(totalSuccess);
        if (payment.getMontant().compareTo(prixRestant) > 0) {
            throw new PaymentValidationException(
                    "Le montant (" + payment.getMontant() +
                    ") dépasse le montant restant dû (" + prixRestant +
                    ") pour la réservation " + reservation.getCode()
            );
        }

        payment.setStatut(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        if (payment.getReference() == null || payment.getReference().isBlank()) {
            payment.setReference("TICKET-" + payment.getCode());
        }

        Payment saved = paymentRepository.save(payment);

        if (cashReceiptPdfService != null) {
            byte[] receiptBytes = cashReceiptPdfService.generate(saved, reservation);
            if (saved.getId() != null && receiptBytes != null) {
                generatedReceipts.put(saved.getId(), receiptBytes);
            }
        }

        notifyClientAfterConfirmation(reservation);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new PaymentSucceededEvent(saved));
        }

        return paymentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public byte[] generateReceipt(Long paymentId) {
        Long currentUserId = getCurrentUserId();

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        Reservation reservation = reservationRepository.findById(payment.getReservationId())
                .orElseThrow(() -> new ReservationNotFoundException(payment.getReservationId()));

        boolean isOwner = payment.getUserId().equals(currentUserId);
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgency = currentUserService.isAgence() && isVehicleAgency(reservation, currentUserId);

        if (!isOwner && !isAdmin && !isAgency) {
            throw new AccessDeniedException("Accès refusé : vous ne pouvez pas télécharger ce reçu");
        }

        validateCashMethod(payment.getMethode());

        if (payment.getStatut() != PaymentStatus.SUCCESS) {
            throw new InvalidPaymentStatusException("Le reçu PDF n'est disponible que pour un paiement confirmé (SUCCESS)");
        }

        byte[] cached = payment.getId() != null ? generatedReceipts.get(payment.getId()) : null;
        if (cached != null && cached.length > 0) {
            return cached;
        }
        return cashReceiptPdfService.generate(payment, reservation);
    }

    private void validateCashMethod(PaymentMethod methode) {
        if (methode != PaymentMethod.CASH_DELIVERY && methode != PaymentMethod.CASH_AGENCY) {
            throw new PaymentValidationException(
                    "La méthode de paiement en espèces doit être CASH_DELIVERY ou CASH_AGENCY"
            );
        }
    }

    private void verifyAgencyOrAdminAccess(Reservation reservation, Long currentUserId) {
        if (currentUserService.isAdmin()) {
            return;
        }
        if (currentUserService.isAgence() && isVehicleAgency(reservation, currentUserId)) {
            return;
        }
        throw new AccessDeniedException("Accès refusé : seule l'agence propriétaire ou un administrateur peut confirmer un paiement en espèces");
    }

    private boolean isVehicleAgency(Reservation reservation, Long currentUserId) {
        return vehicleRepository.findById(reservation.getVehicleId())
                .map(Vehicle::getAgenceId)
                .map(agenceId -> agenceId.equals(currentUserId))
                .orElse(false);
    }

    private void notifyClientAfterConfirmation(Reservation reservation) {
        if (notificationService == null || reservation == null) {
            return;
        }
        try {
            notificationService.sendConfirmation(reservation);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification client après confirmation du paiement espèces [reservationId={}] : {}",
                    reservation.getId(), e.getMessage());
        }
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
}
