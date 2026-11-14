package ma.locavite.payment.service;

import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.dto.PaymentFilter;
import ma.locavite.payment.dto.PaymentRequest;
import ma.locavite.payment.dto.PaymentResponse;
import ma.locavite.payment.event.PaymentFailedEvent;
import ma.locavite.payment.event.PaymentInitiatedEvent;
import ma.locavite.payment.event.PaymentSucceededEvent;
import ma.locavite.payment.exception.InvalidPaymentStatusException;
import ma.locavite.payment.exception.PaymentNotFoundException;
import ma.locavite.payment.exception.PaymentValidationException;
import ma.locavite.payment.mapper.PaymentMapper;
import ma.locavite.payment.repository.PaymentRepository;
import ma.locavite.payment.repository.PaymentSpecifications;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.exception.ReservationNotFoundException;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class PaymentService {

    private static final String CODE_PREFIX = "PAY-";
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final PaymentMapper paymentMapper;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentService(
            PaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            VehicleRepository vehicleRepository,
            PaymentMapper paymentMapper,
            CurrentUserService currentUserService,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.paymentMapper = paymentMapper;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
    }

    public PaymentResponse initiate(PaymentRequest request) {
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

        Payment payment = new Payment();
        payment.setCode(generateUniqueCode());
        payment.setReservationId(reservation.getId());
        payment.setUserId(reservation.getUserId());
        payment.setMontant(request.montant());
        payment.setDevise((request.devise() != null && !request.devise().isBlank()) ? request.devise().trim() : "MAD");
        payment.setMethode(request.methode());
        payment.setStatut(PaymentStatus.PENDING);
        payment.setReference(request.reference());
        payment.setMetadata(request.metadata());

        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishEvent(new PaymentInitiatedEvent(saved));

        return paymentMapper.toResponse(saved);
    }

    public PaymentResponse confirm(Long paymentId, String transactionId, String reference, String metadata) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (!payment.getStatut().canTransitionTo(PaymentStatus.SUCCESS)) {
            throw new InvalidPaymentStatusException("Impossible de confirmer un paiement avec le statut : " + payment.getStatut());
        }

        Reservation reservation = reservationRepository.findByIdForUpdate(payment.getReservationId())
                .orElseThrow(() -> new ReservationNotFoundException(payment.getReservationId()));

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
        if (transactionId != null && !transactionId.isBlank()) {
            payment.setTransactionId(transactionId);
        }
        if (reference != null && !reference.isBlank()) {
            payment.setReference(reference);
        }
        if (metadata != null && !metadata.isBlank()) {
            payment.setMetadata(metadata);
        }

        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishEvent(new PaymentSucceededEvent(saved));

        return paymentMapper.toResponse(saved);
    }

    public PaymentResponse confirm(Long paymentId) {
        return confirm(paymentId, null, null, null);
    }

    public PaymentResponse fail(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (!payment.getStatut().canTransitionTo(PaymentStatus.FAILED)) {
            throw new InvalidPaymentStatusException("Impossible d'échouer un paiement avec le statut : " + payment.getStatut());
        }

        payment.setStatut(PaymentStatus.FAILED);
        if (reason != null && !reason.isBlank() && (payment.getMetadata() == null || payment.getMetadata().isBlank())) {
            payment.setMetadata("{\"failureReason\":\"" + reason + "\"}");
        }

        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishEvent(new PaymentFailedEvent(saved));

        return paymentMapper.toResponse(saved);
    }

    public PaymentResponse fail(Long paymentId) {
        return fail(paymentId, null);
    }

    public PaymentResponse cancel(Long paymentId) {
        Long currentUserId = getCurrentUserId();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (!payment.getUserId().equals(currentUserId) && !currentUserService.isAdmin()) {
            throw new AccessDeniedException("Accès refusé : vous ne pouvez pas annuler ce paiement");
        }

        if (!payment.getStatut().canTransitionTo(PaymentStatus.CANCELLED)) {
            throw new InvalidPaymentStatusException("Impossible d'annuler un paiement avec le statut : " + payment.getStatut());
        }

        payment.setStatut(PaymentStatus.CANCELLED);
        Payment saved = paymentRepository.save(payment);

        return paymentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        Long currentUserId = getCurrentUserId();
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        boolean isOwner = payment.getUserId().equals(currentUserId);
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgency = currentUserService.isAgence() && isVehicleAgency(payment);

        if (!isOwner && !isAdmin && !isAgency) {
            throw new AccessDeniedException("Accès refusé : vous ne pouvez pas consulter ce paiement");
        }

        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMe(Pageable pageable) {
        Long currentUserId = getCurrentUserId();
        return paymentRepository.findByUserId(currentUserId, pageable)
                .map(paymentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAll(PaymentFilter filter, Pageable pageable) {
        Long currentUserId = getCurrentUserId();

        if (currentUserService.isAdmin()) {
            Specification<Payment> spec = PaymentSpecifications.withFilter(filter, null);
            return paymentRepository.findAll(spec, pageable).map(paymentMapper::toResponse);
        }

        if (currentUserService.isAgence()) {
            List<Long> agencyVehicleIds = vehicleRepository.findIdsByAgenceId(currentUserId);
            if (agencyVehicleIds == null || agencyVehicleIds.isEmpty()) {
                return Page.empty(pageable);
            }
            List<Long> agencyReservationIds = reservationRepository.findIdsByVehicleIdIn(agencyVehicleIds);
            if (agencyReservationIds == null || agencyReservationIds.isEmpty()) {
                return Page.empty(pageable);
            }
            Specification<Payment> spec = PaymentSpecifications.withFilter(filter, agencyReservationIds);
            return paymentRepository.findAll(spec, pageable).map(paymentMapper::toResponse);
        }

        throw new AccessDeniedException("Accès réservé aux agences et administrateurs");
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getByReservationId(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    private boolean isVehicleAgency(Payment payment) {
        return reservationRepository.findById(payment.getReservationId())
                .flatMap(res -> vehicleRepository.findById(res.getVehicleId()))
                .map(Vehicle::getAgenceId)
                .map(agenceId -> agenceId.equals(getCurrentUserId()))
                .orElse(false);
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
