package ma.locavite.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.document.service.MinioService;
import ma.locavite.mail.repository.EmailLogRepository;
import ma.locavite.notification.service.NotificationService;
import ma.locavite.payment.cmi.exception.CmiUnavailableException;
import ma.locavite.payment.cmi.service.CmiClient;
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
import ma.locavite.payment.exception.PaymentValidationException;
import ma.locavite.payment.repository.PaymentRepository;
import ma.locavite.payment.repository.RefundRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRefundServiceTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CmiClient cmiClient;

    @Mock
    private MinioService minioService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailLogRepository emailLogRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AvoirPdfService avoirPdfService;
    private RefundService refundService;

    private User sampleClient;
    private Vehicle sampleVehicle;
    private Reservation sampleReservation;
    private Payment sampleCmiPayment;
    private Payment sampleBankTransferPayment;

    @BeforeEach
    void setUp() {
        avoirPdfService = new AvoirPdfService(new PdfFontPathResolver(new ContractProperties()), minioService);
        refundService = new RefundService(
                refundRepository,
                paymentRepository,
                reservationRepository,
                vehicleRepository,
                userRepository,
                currentUserService,
                cmiClient,
                avoirPdfService,
                notificationService,
                mailSender,
                emailLogRepository,
                eventPublisher,
                new ObjectMapper()
        );

        sampleClient = new User();
        sampleClient.setId(10L);
        sampleClient.setNom("El Amrani");
        sampleClient.setPrenom("Youssef");
        sampleClient.setEmail("youssef.elamrani@test.com");
        sampleClient.setTelephone("+212612345678");

        sampleVehicle = new Vehicle();
        sampleVehicle.setId(5L);
        sampleVehicle.setAgenceId(20L);
        sampleVehicle.setModele("Peugeot 208");

        sampleReservation = new Reservation();
        sampleReservation.setId(100L);
        sampleReservation.setCode("LV-REF001");
        sampleReservation.setUserId(10L);
        sampleReservation.setVehicleId(5L);
        sampleReservation.setDateDebut(LocalDate.now().plusDays(5));
        sampleReservation.setDateFin(LocalDate.now().plusDays(8));
        sampleReservation.setPrixTotal(new BigDecimal("1200.00"));
        sampleReservation.setStatut(ReservationStatus.CONFIRMED);

        sampleCmiPayment = new Payment();
        sampleCmiPayment.setId(300L);
        sampleCmiPayment.setCode("PAY-CMI300");
        sampleCmiPayment.setReservationId(100L);
        sampleCmiPayment.setUserId(10L);
        sampleCmiPayment.setMontant(new BigDecimal("1200.00"));
        sampleCmiPayment.setDevise("MAD");
        sampleCmiPayment.setMethode(PaymentMethod.CMI);
        sampleCmiPayment.setStatut(PaymentStatus.SUCCESS);
        sampleCmiPayment.setTransactionId("CMI-TX-900");
        sampleCmiPayment.setPaidAt(Instant.now());

        sampleBankTransferPayment = new Payment();
        sampleBankTransferPayment.setId(301L);
        sampleBankTransferPayment.setCode("PAY-VIR301");
        sampleBankTransferPayment.setReservationId(100L);
        sampleBankTransferPayment.setUserId(10L);
        sampleBankTransferPayment.setMontant(new BigDecimal("1200.00"));
        sampleBankTransferPayment.setDevise("MAD");
        sampleBankTransferPayment.setMethode(PaymentMethod.BANK_TRANSFER);
        sampleBankTransferPayment.setStatut(PaymentStatus.SUCCESS);
        sampleBankTransferPayment.setPaidAt(Instant.now());
    }

    @Test
    @DisplayName("Test : remboursement total → statut REFUNDED (CMI via API)")
    void create_fullRefund_shouldTransitionPaymentToRefundedAndCompleteRefund() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(20L));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgence()).thenReturn(true);
        when(paymentRepository.findById(300L)).thenReturn(Optional.of(sampleCmiPayment));
        when(reservationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleReservation));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(sampleVehicle));
        when(refundRepository.sumCompletedAmountByPaymentId(300L)).thenReturn(BigDecimal.ZERO);
        when(cmiClient.executeRefund(eq(sampleCmiPayment), any(BigDecimal.class), anyString()))
                .thenReturn("CMI-REF-PAY-CMI300-001");
        when(refundRepository.getNextAvoirSequenceValue()).thenReturn(1L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleClient));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> {
            Refund r = inv.getArgument(0);
            if (r.getId() == null) r.setId(1L);
            return r;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundRequest request = new RefundRequest(new BigDecimal("1200.00"), "Annulation plus de 7 jours avant le départ");
        RefundResponse response = refundService.create(300L, request);

        assertThat(response.paymentStatut()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(sampleCmiPayment.getStatut()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(response.statut()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(response.methode()).isEqualTo(RefundMethod.CMI);
        assertThat(response.montant()).isEqualByComparingTo("1200.00");
        assertThat(response.montantTotalRembourse()).isEqualByComparingTo("1200.00");
        assertThat(response.montantRestant()).isEqualByComparingTo("0.00");
        assertThat(response.reference()).isEqualTo("CMI-REF-PAY-CMI300-001");
        assertThat(response.auditTrail()).contains("INITIATED", "PROCESSING_CMI", "CMI_REFUND_SUCCESS", "COMPLETED");

        ArgumentCaptor<RefundCompletedEvent> eventCaptor = ArgumentCaptor.forClass(RefundCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().payment().getStatut()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(eventCaptor.getValue().refund().getStatut()).isEqualTo(RefundStatus.COMPLETED);
    }

    @Test
    @DisplayName("Test : remboursement partiel → PARTIALLY_REFUNDED + second remboursement partiel → REFUNDED")
    void create_partialRefund_shouldTransitionPaymentToPartiallyRefundedThenRefunded() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(20L));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgence()).thenReturn(true);
        when(paymentRepository.findById(301L)).thenReturn(Optional.of(sampleBankTransferPayment));
        when(reservationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleReservation));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(sampleVehicle));
        when(refundRepository.sumCompletedAmountByPaymentId(301L)).thenReturn(BigDecimal.ZERO);
        when(refundRepository.getNextAvoirSequenceValue()).thenReturn(1L, 2L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleClient));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> {
            Refund r = inv.getArgument(0);
            if (r.getId() == null) r.setId(2L);
            return r;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1er remboursement partiel : 70% = 840.00 MAD sur 1200.00 MAD
        RefundRequest partialRequest = new RefundRequest(new BigDecimal("840.00"), "Annulation à 5 jours (70%)");
        RefundResponse firstResponse = refundService.create(301L, partialRequest);

        assertThat(firstResponse.paymentStatut()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(sampleBankTransferPayment.getStatut()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(firstResponse.statut()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(firstResponse.methode()).isEqualTo(RefundMethod.MANUAL);
        assertThat(firstResponse.montant()).isEqualByComparingTo("840.00");
        assertThat(firstResponse.montantTotalRembourse()).isEqualByComparingTo("840.00");
        assertThat(firstResponse.montantRestant()).isEqualByComparingTo("360.00");

        // 2e remboursement du reliquat : 360.00 MAD (840 + 360 = 1200) -> REFUNDED
        when(refundRepository.sumCompletedAmountByPaymentId(301L)).thenReturn(new BigDecimal("840.00"));
        RefundRequest remainingRequest = new RefundRequest(new BigDecimal("360.00"), "Geste commercial solde restant");
        RefundResponse secondResponse = refundService.create(301L, remainingRequest);

        assertThat(secondResponse.paymentStatut()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(sampleBankTransferPayment.getStatut()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(secondResponse.montantTotalRembourse()).isEqualByComparingTo("1200.00");
        assertThat(secondResponse.montantRestant()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Test : montant > paiement (ou > paiement - déjà remboursé) → 400 PaymentValidationException")
    void create_whenMontantExceedsAvailableBalance_shouldThrowPaymentValidationException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(20L));
        when(currentUserService.isAdmin()).thenReturn(true);
        when(paymentRepository.findById(300L)).thenReturn(Optional.of(sampleCmiPayment));
        when(reservationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleReservation));

        // Cas 1 : Montant (1500.00) > Paiement initial (1200.00)
        when(refundRepository.sumCompletedAmountByPaymentId(300L)).thenReturn(BigDecimal.ZERO);
        RefundRequest tooLargeRequest = new RefundRequest(new BigDecimal("1500.00"), "Montant excessif");

        assertThatThrownBy(() -> refundService.create(300L, tooLargeRequest))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessageContaining("dépasse le montant remboursable restant");

        // Cas 2 : Déjà remboursé 800.00 sur 1200.00, demande de 500.00 (> 400.00 restants)
        sampleCmiPayment.setStatut(PaymentStatus.PARTIALLY_REFUNDED);
        when(refundRepository.sumCompletedAmountByPaymentId(300L)).thenReturn(new BigDecimal("800.00"));
        RefundRequest exceedsRemainingRequest = new RefundRequest(new BigDecimal("500.00"), "Dépasse le reliquat");

        assertThatThrownBy(() -> refundService.create(300L, exceedsRemainingRequest))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessageContaining("400.00");

        verify(refundRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Test : avoir PDF généré et stocké avec le détail du remboursement")
    void create_shouldGenerateAndStoreValidAvoirPdf() throws IOException {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(20L));
        when(currentUserService.isAdmin()).thenReturn(true);
        when(paymentRepository.findById(300L)).thenReturn(Optional.of(sampleCmiPayment));
        when(reservationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleReservation));
        when(refundRepository.sumCompletedAmountByPaymentId(300L)).thenReturn(BigDecimal.ZERO);
        when(cmiClient.executeRefund(eq(sampleCmiPayment), any(BigDecimal.class), anyString()))
                .thenReturn("CMI-REF-777");
        when(refundRepository.getNextAvoirSequenceValue()).thenReturn(7L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleClient));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> {
            Refund r = inv.getArgument(0);
            if (r.getId() == null) r.setId(7L);
            return r;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundResponse response = refundService.create(
                300L,
                new RefundRequest(new BigDecimal("600.00"), "Remboursement partiel 50%")
        );

        assertThat(response.avoirPath()).matches("avoirs/\\d{4}/AV-\\d{4}-0007\\.pdf");

        ArgumentCaptor<byte[]> pdfCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(minioService).putInvoice(eq(response.avoirPath()), pdfCaptor.capture());

        byte[] pdfBytes = pdfCaptor.getValue();
        assertThat(pdfBytes).isNotNull().isNotEmpty();

        try (var pdf = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(pdf).replaceAll("\\s+", " ");
            assertThat(text).contains(
                    "LocaVite SARL - Avoir de remboursement",
                    "PAY-CMI300",
                    "LV-REF001",
                    "Youssef El Amrani",
                    "600.00 MAD",
                    "500.00 MAD",
                    "100.00 MAD",
                    "Remboursement partiel 50%",
                    "PARTIALLY_REFUNDED"
            );
        }
    }

    @Test
    @DisplayName("Test : notification envoyée (email avec avoir PDF + SMS via NotificationService)")
    void create_shouldSendEmailWithAvoirAttachmentAndSmsNotification() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(20L));
        when(currentUserService.isAdmin()).thenReturn(true);
        when(paymentRepository.findById(300L)).thenReturn(Optional.of(sampleCmiPayment));
        when(reservationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleReservation));
        when(refundRepository.sumCompletedAmountByPaymentId(300L)).thenReturn(BigDecimal.ZERO);
        when(cmiClient.executeRefund(eq(sampleCmiPayment), any(BigDecimal.class), anyString()))
                .thenReturn("CMI-REF-888");
        when(refundRepository.getNextAvoirSequenceValue()).thenReturn(8L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleClient));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> {
            Refund r = inv.getArgument(0);
            if (r.getId() == null) r.setId(8L);
            return r;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        refundService.create(300L, new RefundRequest(new BigDecimal("1200.00"), "Annulation totale"));

        verify(mailSender).send(mimeMessage);
        verify(emailLogRepository).save(any());
        verify(notificationService).sendRefund(sampleReservation);
    }

    @Test
    @DisplayName("Test : échec de l'API CMI passe le remboursement au statut FAILED")
    void processCmiRefund_whenCmiFails_shouldSetRefundStatusToFailed() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(20L));
        when(currentUserService.isAdmin()).thenReturn(true);
        when(paymentRepository.findById(300L)).thenReturn(Optional.of(sampleCmiPayment));
        when(reservationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleReservation));
        when(refundRepository.sumCompletedAmountByPaymentId(300L)).thenReturn(BigDecimal.ZERO);
        when(cmiClient.executeRefund(any(), any(), any()))
                .thenThrow(new CmiUnavailableException("Passerelle CMI Refund indisponible"));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> refundService.create(300L, new RefundRequest(new BigDecimal("500.00"), "Annulation")))
                .isInstanceOf(CmiUnavailableException.class);

        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository, atLeastOnce()).save(refundCaptor.capture());
        List<Refund> savedStates = refundCaptor.getAllValues();
        assertThat(savedStates.get(savedStates.size() - 1).getStatut()).isEqualTo(RefundStatus.FAILED);
        assertThat(sampleCmiPayment.getStatut()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("Test : refus de rembourser un paiement PENDING ou une autre agence (400 / 403)")
    void create_whenInvalidStatusOrUnauthorizedAgency_shouldThrowException() {
        // 1. Autre agence (99L != 20L) -> AccessDeniedException
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(99L));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgence()).thenReturn(true);
        when(paymentRepository.findById(300L)).thenReturn(Optional.of(sampleCmiPayment));
        when(reservationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleReservation));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(sampleVehicle));

        assertThatThrownBy(() -> refundService.create(300L, new RefundRequest(new BigDecimal("100.00"), "Test")))
                .isInstanceOf(AccessDeniedException.class);

        // 2. Paiement au statut PENDING -> InvalidPaymentStatusException
        sampleCmiPayment.setStatut(PaymentStatus.PENDING);
        when(currentUserService.isAdmin()).thenReturn(true);

        assertThatThrownBy(() -> refundService.create(300L, new RefundRequest(new BigDecimal("100.00"), "Test")))
                .isInstanceOf(InvalidPaymentStatusException.class);
    }
}
