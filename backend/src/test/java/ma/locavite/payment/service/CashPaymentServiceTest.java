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
import ma.locavite.payment.exception.PaymentValidationException;
import ma.locavite.payment.mapper.PaymentMapper;
import ma.locavite.payment.repository.PaymentRepository;
import ma.locavite.reservation.config.ContractProperties;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.reservation.service.PdfFontPathResolver;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final PaymentMapper paymentMapper = Mappers.getMapper(PaymentMapper.class);
    private CashReceiptPdfService cashReceiptPdfService;
    private CashPaymentService cashPaymentService;

    private Reservation sampleReservation;
    private Vehicle sampleVehicle;
    private Payment sampleCashPayment;

    @BeforeEach
    void setUp() {
        cashReceiptPdfService = new CashReceiptPdfService(new PdfFontPathResolver(new ContractProperties()));
        cashPaymentService = new CashPaymentService(
                paymentRepository,
                reservationRepository,
                vehicleRepository,
                paymentMapper,
                currentUserService,
                cashReceiptPdfService,
                notificationService,
                eventPublisher
        );

        sampleVehicle = new Vehicle();
        sampleVehicle.setId(5L);
        sampleVehicle.setAgenceId(20L);
        sampleVehicle.setModele("Dacia Logan");

        sampleReservation = new Reservation();
        sampleReservation.setId(1L);
        sampleReservation.setCode("LV-CASH01");
        sampleReservation.setUserId(10L);
        sampleReservation.setVehicleId(5L);
        sampleReservation.setPrixTotal(new BigDecimal("1500.00"));
        sampleReservation.setStatut(ReservationStatus.PENDING);

        sampleCashPayment = new Payment();
        sampleCashPayment.setId(200L);
        sampleCashPayment.setCode("PAY-CSH001");
        sampleCashPayment.setReservationId(1L);
        sampleCashPayment.setUserId(10L);
        sampleCashPayment.setMontant(new BigDecimal("1500.00"));
        sampleCashPayment.setDevise("MAD");
        sampleCashPayment.setMethode(PaymentMethod.CASH_DELIVERY);
        sampleCashPayment.setStatut(PaymentStatus.PENDING);
        sampleCashPayment.setCreatedAt(Instant.now());
        sampleCashPayment.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("Test : création paiement cash (CASH_DELIVERY et CASH_AGENCY) → PENDING")
    void createCashPayment_validMethods_shouldCreatePendingPayment() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleReservation));
        when(paymentRepository.sumSuccessAmountByReservationId(1L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.existsByCode(anyString())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(201L);
            return p;
        });

        // 1. Choix CASH_DELIVERY
        PaymentResponse deliveryResponse = cashPaymentService.createCashPayment(
                new CashPaymentRequest(1L, PaymentMethod.CASH_DELIVERY)
        );

        assertThat(deliveryResponse).isNotNull();
        assertThat(deliveryResponse.statut()).isEqualTo(PaymentStatus.PENDING);
        assertThat(deliveryResponse.methode()).isEqualTo(PaymentMethod.CASH_DELIVERY);
        assertThat(deliveryResponse.montant()).isEqualByComparingTo("1500.00");
        assertThat(deliveryResponse.transactionId()).isNull();
        assertThat(deliveryResponse.paidAt()).isNull();

        // 2. Choix CASH_AGENCY
        PaymentResponse agencyResponse = cashPaymentService.createCashPayment(
                new CashPaymentRequest(1L, PaymentMethod.CASH_AGENCY)
        );

        assertThat(agencyResponse).isNotNull();
        assertThat(agencyResponse.statut()).isEqualTo(PaymentStatus.PENDING);
        assertThat(agencyResponse.methode()).isEqualTo(PaymentMethod.CASH_AGENCY);
        assertThat(agencyResponse.montant()).isEqualByComparingTo("1500.00");

        verify(eventPublisher, times(2)).publishEvent(any(PaymentInitiatedEvent.class));
    }

    @Test
    @DisplayName("Test : confirmation agence → SUCCESS + notification au client + reçu PDF généré")
    void confirmCash_byAgence_shouldTransitionToSuccessAndNotifyClientAndGeneratePdf() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(20L));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgence()).thenReturn(true);
        when(paymentRepository.findById(200L)).thenReturn(Optional.of(sampleCashPayment));
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleReservation));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(sampleVehicle));
        when(paymentRepository.sumSuccessAmountByReservationId(1L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = cashPaymentService.confirmCash(200L);

        assertThat(response.statut()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.paidAt()).isNotNull();
        assertThat(response.reference()).startsWith("TICKET-");

        verify(notificationService).sendConfirmation(sampleReservation);

        ArgumentCaptor<PaymentSucceededEvent> eventCaptor = ArgumentCaptor.forClass(PaymentSucceededEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().payment().getStatut()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("Test : reçu PDF généré (ticket de caisse contient les détails du paiement et de la réservation)")
    void generateReceipt_afterConfirmation_shouldReturnValidPdfWithTicketDetails() throws IOException {
        sampleCashPayment.setStatut(PaymentStatus.SUCCESS);
        sampleCashPayment.setPaidAt(Instant.parse("2026-11-14T12:00:00Z"));

        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(20L));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgence()).thenReturn(true);
        when(paymentRepository.findById(200L)).thenReturn(Optional.of(sampleCashPayment));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(sampleReservation));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(sampleVehicle));

        byte[] pdfBytes = cashPaymentService.generateReceipt(200L);

        assertThat(pdfBytes).isNotNull().isNotEmpty();
        try (var pdf = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(pdf).replaceAll("\\s+", " ");
            assertThat(text).contains(
                    "Ticket de caisse",
                    "PAY-CSH001",
                    "LV-CASH01",
                    "CASH_DELIVERY",
                    "1500.00",
                    "SUCCESS"
            );
        }
    }

    @Test
    @DisplayName("Test : client ne peut pas confirmer lui-même → AccessDeniedException (403)")
    void confirmCash_byClient_shouldThrowAccessDeniedException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgence()).thenReturn(false);
        when(paymentRepository.findById(200L)).thenReturn(Optional.of(sampleCashPayment));
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleReservation));

        assertThatThrownBy(() -> cashPaymentService.confirmCash(200L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Accès refusé");

        assertThat(sampleCashPayment.getStatut()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository, never()).save(any());
        verify(notificationService, never()).sendConfirmation(any());
    }

    @Test
    @DisplayName("Test : une autre agence ne peut pas confirmer le paiement → AccessDeniedException (403)")
    void confirmCash_byOtherAgency_shouldThrowAccessDeniedException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(99L));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgence()).thenReturn(true);
        when(paymentRepository.findById(200L)).thenReturn(Optional.of(sampleCashPayment));
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleReservation));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(sampleVehicle));

        assertThatThrownBy(() -> cashPaymentService.confirmCash(200L))
                .isInstanceOf(AccessDeniedException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test : méthode non espèces (CMI) refusée lors de la création cash")
    void createCashPayment_withNonCashMethod_shouldThrowPaymentValidationException() {
        CashPaymentRequest invalidRequest = new CashPaymentRequest(1L, PaymentMethod.CMI);

        assertThatThrownBy(() -> cashPaymentService.createCashPayment(invalidRequest))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessageContaining("CASH_DELIVERY ou CASH_AGENCY");
    }

    @Test
    @DisplayName("Test : machine à états PENDING → SUCCESS interdit la re-confirmation d'un paiement déjà SUCCESS")
    void confirmCash_whenAlreadySuccess_shouldThrowInvalidPaymentStatusException() {
        sampleCashPayment.setStatut(PaymentStatus.SUCCESS);
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(20L));
        when(currentUserService.isAdmin()).thenReturn(true);
        when(paymentRepository.findById(200L)).thenReturn(Optional.of(sampleCashPayment));
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleReservation));

        assertThatThrownBy(() -> cashPaymentService.confirmCash(200L))
                .isInstanceOf(InvalidPaymentStatusException.class)
                .hasMessageContaining("Impossible de confirmer un paiement en espèces avec le statut : SUCCESS");
    }
}
