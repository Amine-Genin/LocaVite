package ma.locavite.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.document.service.MinioService;
import ma.locavite.mail.repository.EmailLogRepository;
import ma.locavite.payment.domain.Invoice;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentMethod;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.event.PaymentSucceededEvent;
import ma.locavite.payment.exception.InvalidPaymentStatusException;
import ma.locavite.payment.repository.InvoiceRepository;
import ma.locavite.payment.repository.PaymentRepository;
import ma.locavite.reservation.config.ContractProperties;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.reservation.service.PdfFontPathResolver;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.thymeleaf.TemplateEngine;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private MinioService minioService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private EmailLogRepository emailLogRepository;

    private InvoiceService invoiceService;
    private InvoicePaymentListener invoicePaymentListener;

    private User client;
    private Vehicle vehicle;
    private Reservation reservation;
    private Payment successPayment;

    @BeforeEach
    void setUp() {
        ContractProperties contractProperties = new ContractProperties();
        contractProperties.setPublicBaseUrl("http://localhost:8081");
        PdfFontPathResolver fontPathResolver = new PdfFontPathResolver(contractProperties);

        invoiceService = new InvoiceService(
                invoiceRepository,
                paymentRepository,
                reservationRepository,
                userRepository,
                vehicleRepository,
                minioService,
                currentUserService,
                mailSender,
                templateEngine,
                emailLogRepository,
                contractProperties,
                fontPathResolver,
                new ObjectMapper()
        );
        invoicePaymentListener = new InvoicePaymentListener(invoiceService);

        client = new User();
        client.setId(10L);
        client.setNom("Bennani");
        client.setPrenom("Karim");
        client.setEmail("karim.bennani@test.com");
        client.setTelephone("+212611223344");

        Brand brand = new Brand();
        brand.setName("Renault");
        vehicle = new Vehicle();
        vehicle.setId(5L);
        vehicle.setBrand(brand);
        vehicle.setModele("Clio 5");
        vehicle.setMatricule("54321-B-7");
        vehicle.setAgenceId(20L);

        reservation = new Reservation();
        reservation.setId(100L);
        reservation.setCode("LV-INV001");
        reservation.setUserId(10L);
        reservation.setVehicleId(5L);
        reservation.setDateDebut(LocalDate.of(2026, 11, 15));
        reservation.setDateFin(LocalDate.of(2026, 11, 18)); // 3 jours
        reservation.setLieuPriseEnCharge("Casablanca");
        reservation.setLieuRetour("Casablanca");
        reservation.setPrixTotal(new BigDecimal("1200.00"));
        reservation.setStatut(ReservationStatus.CONFIRMED);

        successPayment = new Payment();
        successPayment.setId(500L);
        successPayment.setCode("PAY-INV500");
        successPayment.setReservationId(100L);
        successPayment.setUserId(10L);
        successPayment.setMontant(new BigDecimal("1200.00"));
        successPayment.setDevise("MAD");
        successPayment.setMethode(PaymentMethod.CMI);
        successPayment.setStatut(PaymentStatus.SUCCESS);
        successPayment.setPaidAt(Instant.parse("2026-11-14T15:00:00Z"));
        successPayment.setMetadata("{\"clientIce\":\"001122334455667\",\"clientAddress\":\"45 Avenue Hassan II, Rabat\"}");
    }

    @Test
    @DisplayName("Test : facture générée automatiquement après paiement SUCCESS + stockage MinIO + envoi email")
    void onPaymentSucceeded_shouldGenerateInvoiceAndUploadToMinioAndSendEmail() {
        when(invoiceRepository.findByPaymentId(500L)).thenReturn(Optional.empty());
        when(invoiceRepository.getNextSequenceValue()).thenReturn(1L);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(userRepository.findById(10L)).thenReturn(Optional.of(client));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        invoicePaymentListener.onPaymentSucceeded(new PaymentSucceededEvent(successPayment));

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice saved = invoiceCaptor.getValue();

        assertThat(saved.getNumero()).matches("FA-\\d{4}-0001");
        assertThat(saved.getPaymentId()).isEqualTo(500L);
        assertThat(saved.getMontantTTC()).isEqualByComparingTo("1200.00");
        assertThat(saved.getMontantHT()).isEqualByComparingTo("1000.00");
        assertThat(saved.getMontantTVA()).isEqualByComparingTo("200.00");
        assertThat(saved.getCheminPdf()).endsWith("/" + saved.getNumero() + ".pdf");
        assertThat(saved.getDateEmission()).isNotNull();

        verify(minioService).putInvoice(eq(saved.getCheminPdf()), any(byte[].class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Test : numéro séquentiel sans trou (FA-2026-0001, FA-2026-0002, FA-2026-0003)")
    void generate_multipleInvoices_shouldProduceSequentialNumbersWithoutGaps() {
        AtomicLong seq = new AtomicLong(0);
        when(invoiceRepository.getNextSequenceValue()).thenAnswer(inv -> seq.incrementAndGet());
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(userRepository.findById(10L)).thenReturn(Optional.of(client));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment p1 = copyPayment(501L, "PAY-SEQ001");
        Payment p2 = copyPayment(502L, "PAY-SEQ002");
        Payment p3 = copyPayment(503L, "PAY-SEQ003");

        Invoice inv1 = invoiceService.generate(p1);
        Invoice inv2 = invoiceService.generate(p2);
        Invoice inv3 = invoiceService.generate(p3);

        assertThat(inv1.getNumero()).endsWith("-0001");
        assertThat(inv2.getNumero()).endsWith("-0002");
        assertThat(inv3.getNumero()).endsWith("-0003");
    }

    @Test
    @DisplayName("Test : contenu correct de la facture PDF/A-3 (parsing PDFBox)")
    void generate_shouldCreatePdfA3WithAllRequiredFields() throws IOException {
        when(invoiceRepository.findByPaymentId(500L)).thenReturn(Optional.empty());
        when(invoiceRepository.getNextSequenceValue()).thenReturn(42L);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(userRepository.findById(10L)).thenReturn(Optional.of(client));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice invoice = invoiceService.generate(successPayment, ContractLanguage.FR);

        ArgumentCaptor<byte[]> pdfCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(minioService).putInvoice(eq(invoice.getCheminPdf()), pdfCaptor.capture());

        byte[] pdfBytes = pdfCaptor.getValue();
        assertThat(pdfBytes).isNotNull().isNotEmpty();

        // Vérification de la conformité PDF/A-3 dans les métadonnées/flux et du contenu via PDFBox
        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(1);
            assertThat(pdf.getDocumentCatalog().getOutputIntents()).isNotEmpty();

            String text = new PDFTextStripper().getText(pdf).replaceAll("\\s+", " ");
            assertThat(text).contains(
                    // En-tête LocaVite + ICE + RC + IF + Patente + adresse + téléphone
                    "LocaVite SARL",
                    InvoiceService.COMPANY_ICE,
                    InvoiceService.COMPANY_RC,
                    InvoiceService.COMPANY_IF,
                    InvoiceService.COMPANY_PATENTE,
                    "120 Boulevard Zerktouni",
                    "+212 5 22 00 00 00",
                    // Numéro de facture séquentiel
                    invoice.getNumero(),
                    // Informations client (nom, ICE, adresse)
                    "Karim Bennani",
                    "001122334455667",
                    "45 Avenue Hassan II, Rabat",
                    // Détail : description, quantité, prix unitaire, TVA 20%, total HT, total TTC
                    "LV-INV001",
                    "Renault Clio 5",
                    "1000.00 MAD",
                    "200.00 MAD",
                    "TVA 20%",
                    "1200.00 MAD",
                    // Mode de paiement
                    "CMI",
                    "PAY-INV500",
                    // Mentions légales & QR code
                    "article 145",
                    "PDF/A-3",
                    "QR code de vérification"
            );
        }

        // Vérification également du support multilingue AR
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(paymentRepository.findById(500L)).thenReturn(Optional.of(successPayment));
        when(invoiceRepository.findByPaymentId(500L)).thenReturn(Optional.of(invoice));

        byte[] arPdfBytes = invoiceService.downloadInvoicePdf(500L, ContractLanguage.AR);
        assertThat(arPdfBytes).isNotNull().isNotEmpty();
        try (PDDocument arPdf = Loader.loadPDF(arPdfBytes)) {
            assertThat(arPdf.getDocumentCatalog().getLanguage()).isEqualTo("ar-MA");
            String arText = new PDFTextStripper().getText(arPdf).replaceAll("\\s+", " ");
            assertThat(arText).contains(invoice.getNumero(), "1200.00 MAD");
        }
    }

    @Test
    @DisplayName("Test : téléchargement par propriétaire OK")
    void downloadInvoicePdf_byOwner_shouldReturnPdfBytes() {
        Invoice invoice = new Invoice(
                1L, "FA-2026-0001", 500L,
                new BigDecimal("1000.00"), new BigDecimal("200.00"), new BigDecimal("1200.00"),
                "invoices/2026/FA-2026-0001.pdf", Instant.now()
        );
        byte[] samplePdf = "%PDF-1.7 sample".getBytes(StandardCharsets.UTF_8);

        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(paymentRepository.findById(500L)).thenReturn(Optional.of(successPayment));
        when(invoiceRepository.findByPaymentId(500L)).thenReturn(Optional.of(invoice));
        when(minioService.getInvoice("invoices/2026/FA-2026-0001.pdf")).thenReturn(samplePdf);

        byte[] result = invoiceService.downloadInvoicePdf(500L);

        assertThat(result).isEqualTo(samplePdf);
    }

    @Test
    @DisplayName("Test : téléchargement par autre client → 403 AccessDeniedException")
    void downloadInvoicePdf_byOtherClient_shouldThrowAccessDeniedException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(999L));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isAgence()).thenReturn(false);
        when(paymentRepository.findById(500L)).thenReturn(Optional.of(successPayment));

        assertThatThrownBy(() -> invoiceService.downloadInvoicePdf(500L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Accès refusé");
    }

    @Test
    @DisplayName("Test : refus de générer une facture pour un paiement non confirmé (PENDING)")
    void generate_whenPaymentNotSuccess_shouldThrowInvalidPaymentStatusException() {
        successPayment.setStatut(PaymentStatus.PENDING);

        assertThatThrownBy(() -> invoiceService.generate(successPayment))
                .isInstanceOf(InvalidPaymentStatusException.class);
    }

    private Payment copyPayment(Long id, String code) {
        Payment p = new Payment();
        p.setId(id);
        p.setCode(code);
        p.setReservationId(100L);
        p.setUserId(10L);
        p.setMontant(new BigDecimal("1200.00"));
        p.setDevise("MAD");
        p.setMethode(PaymentMethod.CMI);
        p.setStatut(PaymentStatus.SUCCESS);
        p.setPaidAt(Instant.now());
        return p;
    }
}
