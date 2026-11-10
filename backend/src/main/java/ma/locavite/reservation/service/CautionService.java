package ma.locavite.reservation.service;

import ma.locavite.reservation.domain.CautionStatus;
import ma.locavite.reservation.domain.CautionTransaction;
import ma.locavite.reservation.domain.CautionTransactionType;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.dto.CautionRequest;
import ma.locavite.reservation.dto.CautionTransactionResponse;
import ma.locavite.reservation.exception.ReservationNotFoundException;
import ma.locavite.reservation.repository.CautionTransactionRepository;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CautionService {

    private final ReservationRepository reservationRepository;
    private final CautionTransactionRepository transactionRepository;
    private final VehicleRepository vehicleRepository;
    private final CurrentUserService currentUserService;
    private final CautionReceiptPdfService receiptPdfService;

    public CautionService(
            ReservationRepository reservationRepository,
            CautionTransactionRepository transactionRepository,
            VehicleRepository vehicleRepository,
            CurrentUserService currentUserService,
            CautionReceiptPdfService receiptPdfService) {
        this.reservationRepository = reservationRepository;
        this.transactionRepository = transactionRepository;
        this.vehicleRepository = vehicleRepository;
        this.currentUserService = currentUserService;
        this.receiptPdfService = receiptPdfService;
    }

    @Transactional
    public CautionTransactionResponse update(Long reservationId, CautionRequest request) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
        verifyAgencyAccess(reservation);
        BigDecimal remaining = remainingAmount(reservationId);
        CautionStatus status;

        switch (request.type()) {
            case HOLD -> {
                if (reservation.getCautionStatut() != CautionStatus.PENDING) {
                    throw new IllegalStateException("La caution a deja ete bloquee ou traitee");
                }
                if (request.montant().compareTo(reservation.getCaution()) != 0) {
                    throw new IllegalArgumentException("Le montant bloque doit etre egal au montant de la caution");
                }
                status = CautionStatus.HELD;
            }
            case RELEASE -> {
                if (reservation.getCautionStatut() != CautionStatus.HELD
                        && reservation.getCautionStatut() != CautionStatus.PARTIALLY_RELEASED) {
                    throw new IllegalStateException("Seule une caution bloquee peut etre liberee");
                }
                validateNotAboveRemaining(request.montant(), remaining);
                status = request.montant().compareTo(remaining) == 0
                        ? CautionStatus.RELEASED : CautionStatus.PARTIALLY_RELEASED;
            }
            case CAPTURE -> {
                if (reservation.getCautionStatut() != CautionStatus.HELD
                        && reservation.getCautionStatut() != CautionStatus.PARTIALLY_RELEASED) {
                    throw new IllegalStateException("Seule une caution bloquee peut etre retenue");
                }
                if (request.montant().compareTo(remaining) != 0) {
                    throw new IllegalArgumentException(
                            "Liberez d'abord la part restituee : le montant retenu doit correspondre au solde restant");
                }
                status = CautionStatus.CAPTURED;
            }
            default -> throw new IllegalArgumentException("Operation de caution non prise en charge");
        }

        reservation.setCautionStatut(status);
        reservationRepository.save(reservation);
        CautionTransaction transaction = new CautionTransaction();
        transaction.setReservation(reservation);
        transaction.setMontant(request.montant());
        transaction.setType(request.type());
        transaction.setStatut(status);
        transaction.setNote(request.note());
        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public List<CautionTransactionResponse> getClientHistory(Long clientId) {
        List<CautionTransaction> transactions = transactionRepository.findByReservationUserIdOrderByDateDesc(clientId);
        if (!currentUserService.isAdmin()) {
            Long currentUserId = currentUserService.getCurrentUserId()
                    .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifie"));
            List<Long> agencyVehicleIds = vehicleRepository.findIdsByAgenceId(currentUserId);
            transactions = transactions.stream()
                    .filter(transaction -> agencyVehicleIds.contains(transaction.getReservation().getVehicleId()))
                    .toList();
        }
        return transactions.stream().map(CautionService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public byte[] generateReceipt(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
        verifyAgencyAccess(reservation);
        List<CautionTransaction> transactions =
                transactionRepository.findByReservationIdOrderByDateDesc(reservationId);
        if (transactions.isEmpty()) {
            throw new IllegalStateException("Aucun mouvement de caution n'est disponible pour cette reservation");
        }
        return receiptPdfService.generate(reservation, transactions);
    }

    private BigDecimal remainingAmount(Long reservationId) {
        return transactionRepository.findByReservationIdOrderByDateDesc(reservationId).stream()
                .map(transaction -> switch (transaction.getType()) {
                    case HOLD -> transaction.getMontant();
                    case RELEASE, CAPTURE -> transaction.getMontant().negate();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateNotAboveRemaining(BigDecimal amount, BigDecimal remaining) {
        if (amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("Le montant depasse le solde de caution restant");
        }
    }

    private void verifyAgencyAccess(Reservation reservation) {
        if (currentUserService.isAdmin()) {
            return;
        }
        Long currentUserId = currentUserService.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifie"));
        Vehicle vehicle = vehicleRepository.findById(reservation.getVehicleId())
                .orElseThrow(() -> new AccessDeniedException("Vehicule de la reservation introuvable"));
        if (!vehicle.getAgenceId().equals(currentUserId)) {
            throw new AccessDeniedException("Cette reservation ne depend pas de votre agence");
        }
    }

    private static CautionTransactionResponse toResponse(CautionTransaction transaction) {
        return new CautionTransactionResponse(
                transaction.getId(),
                transaction.getReservation().getId(),
                transaction.getMontant(),
                transaction.getType(),
                transaction.getStatut(),
                transaction.getDate(),
                transaction.getNote());
    }
}
