package ma.locavite.payment.service;

import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentMethod;
import ma.locavite.payment.domain.PaymentStatus;
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
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.exception.ReservationNotFoundException;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final PaymentMapper paymentMapper = Mappers.getMapper(PaymentMapper.class);

    private PaymentService paymentService;

    private Reservation sampleReservation;
    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                reservationRepository,
                vehicleRepository,
                paymentMapper,
                currentUserService,
                eventPublisher
        );

        sampleReservation = new Reservation();
        sampleReservation.setId(1L);
        sampleReservation.setCode("LV-ABC123");
        sampleReservation.setUserId(10L);
        sampleReservation.setVehicleId(5L);
        sampleReservation.setPrixTotal(new BigDecimal("1500.00"));
        sampleReservation.setStatut(ReservationStatus.PENDING);

        samplePayment = new Payment();
        samplePayment.setId(100L);
        samplePayment.setCode("PAY-XYZ987");
        samplePayment.setReservationId(1L);
        samplePayment.setUserId(10L);
        samplePayment.setMontant(new BigDecimal("500.00"));
        samplePayment.setDevise("MAD");
        samplePayment.setMethode(PaymentMethod.CMI);
        samplePayment.setStatut(PaymentStatus.PENDING);
        samplePayment.setCreatedAt(Instant.now());
        samplePayment.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("Création paiement valide → statut PENDING et émission PaymentInitiatedEvent")
    void initiate_validPayment_shouldReturnPendingAndPublishEvent() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleReservation));
        when(paymentRepository.sumSuccessAmountByReservationId(1L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.existsByCode(anyString())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment toSave = invocation.getArgument(0);
            toSave.setId(101L);
            return toSave;
        });

        PaymentRequest request = new PaymentRequest(
                1L,
                new BigDecimal("500.00"),
                "MAD",
                PaymentMethod.CMI,
                "CMI-REF-1",
                "{\"session\":\"abc\"}"
        );

        PaymentResponse response = paymentService.initiate(request);

        assertThat(response).isNotNull();
        assertThat(response.statut()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.montant()).isEqualByComparingTo("500.00");
        assertThat(response.devise()).isEqualTo("MAD");
        assertThat(response.methode()).isEqualTo(PaymentMethod.CMI);
        assertThat(response.code()).startsWith("PAY-");

        ArgumentCaptor<PaymentInitiatedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentInitiatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().payment().getMontant()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Montant > prix restant → 400 (PaymentValidationException)")
    void initiate_whenMontantExceedsRemainingPrice_shouldThrowPaymentValidationException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleReservation));
        when(paymentRepository.sumSuccessAmountByReservationId(1L)).thenReturn(new BigDecimal("1200.00"));

        PaymentRequest request = new PaymentRequest(
                1L,
                new BigDecimal("400.00"),
                "MAD",
                PaymentMethod.CMI,
                null,
                null
        );

        assertThatThrownBy(() -> paymentService.initiate(request))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessageContaining("dépasse le montant restant dû");

        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Création pour réservation inexistante → 404 (ReservationNotFoundException)")
    void initiate_whenReservationDoesNotExist_shouldThrowReservationNotFoundException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(reservationRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        PaymentRequest request = new PaymentRequest(
                999L,
                new BigDecimal("100.00"),
                "MAD",
                PaymentMethod.CMI,
                null,
                null
        );

        assertThatThrownBy(() -> paymentService.initiate(request))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    @DisplayName("Utilisateur non propriétaire de la réservation → 403 (AccessDeniedException)")
    void initiate_whenUserNotOwnerAndNotAdmin_shouldThrowAccessDeniedException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(99L));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleReservation));

        PaymentRequest request = new PaymentRequest(
                1L,
                new BigDecimal("100.00"),
                "MAD",
                PaymentMethod.CMI,
                null,
                null
        );

        assertThatThrownBy(() -> paymentService.initiate(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("vous n'êtes pas autorisé");
    }

    @Test
    @DisplayName("Paiement pour réservation annulée → 400 (PaymentValidationException)")
    void initiate_whenReservationCancelled_shouldThrowPaymentValidationException() {
        sampleReservation.setStatut(ReservationStatus.CANCELLED);
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleReservation));

        PaymentRequest request = new PaymentRequest(
                1L,
                new BigDecimal("100.00"),
                "MAD",
                PaymentMethod.CMI,
                null,
                null
        );

        assertThatThrownBy(() -> paymentService.initiate(request))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessageContaining("annulée");
    }

    @Test
    @DisplayName("Confirmation de paiement valide PENDING → SUCCESS avec paidAt et événement")
    void confirm_validPendingPayment_shouldTransitionToSuccess() {
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleReservation));
        when(paymentRepository.sumSuccessAmountByReservationId(1L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.confirm(100L, "TXN-12345", "REF-EXT-1", "{\"auth\":\"OK\"}");

        assertThat(response.statut()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.paidAt()).isNotNull();
        assertThat(response.transactionId()).isEqualTo("TXN-12345");
        assertThat(response.reference()).isEqualTo("REF-EXT-1");

        ArgumentCaptor<PaymentSucceededEvent> eventCaptor = ArgumentCaptor.forClass(PaymentSucceededEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().payment().getStatut()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("Changement de statut respecte la machine à états : CANCELLED → SUCCESS interdit")
    void confirm_whenPaymentIsAlreadyCancelled_shouldThrowInvalidPaymentStatusException() {
        samplePayment.setStatut(PaymentStatus.CANCELLED);
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> paymentService.confirm(100L))
                .isInstanceOf(InvalidPaymentStatusException.class)
                .hasMessageContaining("Impossible de confirmer un paiement avec le statut : CANCELLED");

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Changement de statut respecte la machine à états : FAILED → SUCCESS interdit")
    void confirm_whenPaymentIsAlreadyFailed_shouldThrowInvalidPaymentStatusException() {
        samplePayment.setStatut(PaymentStatus.FAILED);
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> paymentService.confirm(100L))
                .isInstanceOf(InvalidPaymentStatusException.class)
                .hasMessageContaining("Impossible de confirmer un paiement avec le statut : FAILED");
    }

    @Test
    @DisplayName("Changement de statut respecte la machine à états : SUCCESS → CANCELLED interdit")
    void cancel_whenPaymentIsAlreadySuccess_shouldThrowInvalidPaymentStatusException() {
        samplePayment.setStatut(PaymentStatus.SUCCESS);
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> paymentService.cancel(100L))
                .isInstanceOf(InvalidPaymentStatusException.class)
                .hasMessageContaining("Impossible d'annuler un paiement avec le statut : SUCCESS");
    }

    @Test
    @DisplayName("Annulation valide d'un paiement PENDING → CANCELLED")
    void cancel_validPendingPayment_shouldTransitionToCancelled() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.cancel(100L);

        assertThat(response.statut()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("Échec d'un paiement PENDING → FAILED avec émission d'événement")
    void fail_validPendingPayment_shouldTransitionToFailed() {
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.fail(100L, "Refus bancaire");

        assertThat(response.statut()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.metadata()).contains("Refus bancaire");

        ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().payment().getStatut()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("Client ne voit que ses paiements : getMe retourne uniquement les paiements du client")
    void getMe_shouldReturnOnlyCurrentUserPayments() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(10L));
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(samplePayment), pageable, 1);
        when(paymentRepository.findByUserId(10L, pageable)).thenReturn(page);

        Page<PaymentResponse> result = paymentService.getMe(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).userId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Client ne voit que ses paiements : un autre client ne peut pas voir le paiement d'un tiers via getById")
    void getById_whenUserNotOwnerAndNotAdmin_shouldThrowAccessDeniedException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(99L));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgence()).thenReturn(false);
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> paymentService.getById(100L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("vous ne pouvez pas consulter ce paiement");
    }

    @Test
    @DisplayName("Propriétaire peut consulter son paiement via getById")
    void getById_whenUserIsOwner_shouldReturnPayment() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));

        PaymentResponse response = paymentService.getById(100L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Admin peut consulter n'importe quel paiement via getById")
    void getById_whenUserIsAdmin_shouldReturnPayment() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(99L));
        when(currentUserService.isAdmin()).thenReturn(true);
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));

        PaymentResponse response = paymentService.getById(100L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Agence ne voit que les paiements liés aux réservations de ses véhicules via getAll")
    void getAll_whenAgence_shouldFilterByAgencyVehicles() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(20L));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgence()).thenReturn(true);
        when(vehicleRepository.findIdsByAgenceId(20L)).thenReturn(List.of(5L));
        when(reservationRepository.findIdsByVehicleIdIn(List.of(5L))).thenReturn(List.of(1L));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(samplePayment), pageable, 1);
        when(paymentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<PaymentResponse> result = paymentService.getAll(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).reservationId()).isEqualTo(1L);
    }
}
