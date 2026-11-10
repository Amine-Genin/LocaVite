package ma.locavite.reservation.service;

import ma.locavite.reservation.domain.CautionStatus;
import ma.locavite.reservation.domain.CautionTransaction;
import ma.locavite.reservation.domain.CautionTransactionType;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.dto.CautionRequest;
import ma.locavite.reservation.repository.CautionTransactionRepository;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CautionServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private CautionTransactionRepository transactionRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private CautionReceiptPdfService receiptPdfService;

    private CautionService cautionService;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        cautionService = new CautionService(
                reservationRepository, transactionRepository, vehicleRepository,
                currentUserService, receiptPdfService);
        reservation = new Reservation();
        reservation.setId(42L);
        reservation.setVehicleId(7L);
        reservation.setCaution(new BigDecimal("3000.00"));
        when(reservationRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(reservation));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(12L));
        Vehicle vehicle = new Vehicle();
        vehicle.setAgenceId(12L);
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(vehicle));
        when(transactionRepository.findByReservationIdOrderByDateDesc(42L)).thenReturn(List.of());
        when(transactionRepository.save(any(CautionTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void hold_setsHeldStatusAndRecordsMovement() {
        var result = cautionService.update(42L,
                new CautionRequest(CautionTransactionType.HOLD, new BigDecimal("3000.00"), "Blocage"));

        assertThat(reservation.getCautionStatut()).isEqualTo(CautionStatus.HELD);
        assertThat(result.type()).isEqualTo(CautionTransactionType.HOLD);
        assertThat(result.statut()).isEqualTo(CautionStatus.HELD);
    }

    @Test
    void fullRelease_setsReleasedStatus() {
        reservation.setCautionStatut(CautionStatus.HELD);
        when(transactionRepository.findByReservationIdOrderByDateDesc(42L))
                .thenReturn(List.of(transaction(CautionTransactionType.HOLD, "3000.00")));

        var result = cautionService.update(42L,
                new CautionRequest(CautionTransactionType.RELEASE, new BigDecimal("3000.00"), null));

        assertThat(reservation.getCautionStatut()).isEqualTo(CautionStatus.RELEASED);
        assertThat(result.statut()).isEqualTo(CautionStatus.RELEASED);
    }

    @Test
    void partialRelease_setsPartiallyReleasedStatus() {
        reservation.setCautionStatut(CautionStatus.HELD);
        when(transactionRepository.findByReservationIdOrderByDateDesc(42L))
                .thenReturn(List.of(transaction(CautionTransactionType.HOLD, "3000.00")));

        var result = cautionService.update(42L,
                new CautionRequest(CautionTransactionType.RELEASE, new BigDecimal("1000.00"), null));

        assertThat(reservation.getCautionStatut()).isEqualTo(CautionStatus.PARTIALLY_RELEASED);
        assertThat(result.statut()).isEqualTo(CautionStatus.PARTIALLY_RELEASED);
    }

    @Test
    void capture_recordsRetainedAmountAndSetsCapturedStatus() {
        reservation.setCautionStatut(CautionStatus.HELD);
        when(transactionRepository.findByReservationIdOrderByDateDesc(42L))
                .thenReturn(List.of(transaction(CautionTransactionType.HOLD, "3000.00")));

        var result = cautionService.update(42L,
                new CautionRequest(CautionTransactionType.CAPTURE, new BigDecimal("3000.00"), "Dommages"));

        assertThat(reservation.getCautionStatut()).isEqualTo(CautionStatus.CAPTURED);
        assertThat(result.statut()).isEqualTo(CautionStatus.CAPTURED);
        ArgumentCaptor<CautionTransaction> movement = ArgumentCaptor.forClass(CautionTransaction.class);
        verify(transactionRepository).save(movement.capture());
        assertThat(movement.getValue().getType()).isEqualTo(CautionTransactionType.CAPTURE);
        assertThat(movement.getValue().getMontant()).isEqualByComparingTo("3000.00");
    }

    private CautionTransaction transaction(CautionTransactionType type, String amount) {
        CautionTransaction transaction = new CautionTransaction();
        transaction.setType(type);
        transaction.setMontant(new BigDecimal(amount));
        return transaction;
    }
}
