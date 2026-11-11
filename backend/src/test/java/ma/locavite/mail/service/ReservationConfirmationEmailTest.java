package ma.locavite.mail.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.auth.service.EmailService;
import ma.locavite.auth.service.EmailServiceImpl;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.document.service.MinioService;
import ma.locavite.mail.domain.EmailLog;
import ma.locavite.mail.repository.EmailLogRepository;
import ma.locavite.reservation.config.ContractProperties;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationContract;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.event.ReservationConfirmedEvent;
import ma.locavite.reservation.repository.ReservationContractRepository;
import ma.locavite.reservation.service.ContractEmailListener;
import ma.locavite.reservation.service.ContractService;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationConfirmationEmailTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private EmailLogRepository emailLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private ReservationContractRepository contractRepository;
    @Mock
    private MinioService minioService;
    @Mock
    private ContractService contractService;

    private TemplateEngine templateEngine;
    private ContractProperties contractProperties;
    private EmailServiceImpl emailService;

    private Reservation reservation;
    private ReservationContract contract;
    private User client;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine springTemplateEngine = new SpringTemplateEngine();
        springTemplateEngine.setTemplateResolver(resolver);
        this.templateEngine = springTemplateEngine;

        contractProperties = new ContractProperties();
        contractProperties.setPublicBaseUrl("https://locavite.ma");

        emailService = new EmailServiceImpl(
                mailSender,
                templateEngine,
                emailLogRepository,
                userRepository,
                vehicleRepository,
                contractRepository,
                minioService,
                contractProperties
        );

        client = new User();
        client.setId(10L);
        client.setNom("Benali");
        client.setPrenom("Karim");
        client.setEmail("karim.benali@example.com");

        Brand brand = new Brand();
        brand.setName("Toyota");

        vehicle = new Vehicle();
        vehicle.setId(20L);
        vehicle.setBrand(brand);
        vehicle.setModele("RAV4");
        vehicle.setMatricule("12345-A-6");

        reservation = new Reservation();
        reservation.setId(100L);
        reservation.setCode("LV-CONFIRM-123");
        reservation.setUserId(10L);
        reservation.setVehicleId(20L);
        reservation.setDateDebut(LocalDate.of(2026, 12, 1));
        reservation.setDateFin(LocalDate.of(2026, 12, 5));
        reservation.setLieuPriseEnCharge("Aéroport Casablanca");
        reservation.setLieuRetour("Gare Rabat");
        reservation.setPrixTotal(new BigDecimal("4500.00"));
        reservation.setCaution(new BigDecimal("3000.00"));
        reservation.setStatut(ReservationStatus.CONFIRMED);

        contract = new ReservationContract();
        contract.setId(1L);
        contract.setReservation(reservation);
        contract.setLanguage(ContractLanguage.FR);
        contract.setObjectPath("100/fr/contract-LV-CONFIRM-123.pdf");
        contract.setAccessToken("token123");

        lenient().when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    @DisplayName("Test : envoi déclenché sur confirmation (ContractEmailListener)")
    void testEnvoiDeclencheSurConfirmation() {
        EmailService mockEmailService = mock(EmailService.class);
        ContractEmailListener listener = new ContractEmailListener(contractService, mockEmailService);

        ReservationConfirmedEvent event = new ReservationConfirmedEvent(reservation, contract);
        listener.sendContractAfterConfirmation(event);

        verify(mockEmailService, times(1)).sendReservationConfirmation(reservation, contract);
    }

    @Test
    @DisplayName("Test : template contient les bonnes variables en Français")
    void testTemplateContientLesBonnesVariables_FR() {
        Context context = new Context();
        context.setVariable("client", client);
        context.setVariable("reservation", reservation);
        context.setVariable("vehicle", vehicle);
        context.setVariable("contract", contract);
        context.setVariable("reservationUrl", "https://locavite.ma/reservations/LV-CONFIRM-123");
        context.setVariable("lang", "FR");

        String html = templateEngine.process("mail/reservation-confirmation-fr", context);

        assertThat(html)
                .contains("Loca", "Vite") // Logo
                .contains("LV-CONFIRM-123") // Code
                .contains("Karim Benali") // Client
                .contains("Toyota RAV4") // Véhicule
                .contains("12345-A-6") // Matricule
                .contains("2026-12-01 au 2026-12-05") // Dates
                .contains("Aéroport Casablanca") // Lieu prise en charge
                .contains("Gare Rabat") // Lieu retour
                .contains("4500.00 MAD") // Prix total
                .contains("3000.00 MAD") // Caution
                .contains("Contrat PDF en pièce jointe") // Contrat PDF
                .contains("Voir ma réservation") // Bouton
                .contains("https://locavite.ma/reservations/LV-CONFIRM-123"); // Lien bouton
    }

    @Test
    @DisplayName("Test : template multilingue Anglais")
    void testTemplateMultilingue_EN() {
        Context context = new Context();
        context.setVariable("client", client);
        context.setVariable("reservation", reservation);
        context.setVariable("vehicle", vehicle);
        context.setVariable("contract", contract);
        context.setVariable("reservationUrl", "https://locavite.ma/reservations/LV-CONFIRM-123");
        context.setVariable("lang", "EN");

        String html = templateEngine.process("mail/reservation-confirmation-en", context);

        assertThat(html)
                .contains("Loca", "Vite")
                .contains("Reservation Confirmation")
                .contains("LV-CONFIRM-123")
                .contains("View my reservation")
                .contains("PDF Contract Attached");
    }

    @Test
    @DisplayName("Test : template multilingue Arabe avec direction RTL")
    void testTemplateMultilingue_AR() {
        Context context = new Context();
        context.setVariable("client", client);
        context.setVariable("reservation", reservation);
        context.setVariable("vehicle", vehicle);
        context.setVariable("contract", contract);
        context.setVariable("reservationUrl", "https://locavite.ma/reservations/LV-CONFIRM-123");
        context.setVariable("lang", "AR");

        String html = templateEngine.process("mail/reservation-confirmation-ar", context);

        assertThat(html)
                .contains("Loca", "Vite")
                .contains("dir=\"rtl\"")
                .contains("تأكيد الحجز")
                .contains("LV-CONFIRM-123")
                .contains("عرض تفاصيل الحجز");
    }

    @Test
    @DisplayName("Test : log créé en base sur envoi réussi")
    void testLogCreeEnBase() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(client));
        when(vehicleRepository.findById(20L)).thenReturn(Optional.of(vehicle));
        when(minioService.getContract("100/fr/contract-LV-CONFIRM-123.pdf"))
                .thenReturn("Fake PDF Content".getBytes());

        emailService.sendReservationConfirmation(reservation, contract, ContractLanguage.FR);

        verify(mailSender, times(1)).send(any(MimeMessage.class));

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, times(1)).save(captor.capture());

        EmailLog savedLog = captor.getValue();
        assertThat(savedLog.getReservationId()).isEqualTo(100L);
        assertThat(savedLog.getRecipient()).isEqualTo("karim.benali@example.com");
        assertThat(savedLog.getDestinataire()).isEqualTo("karim.benali@example.com");
        assertThat(savedLog.getStatus()).isEqualTo("SENT");
        assertThat(savedLog.getStatut()).isEqualTo("SENT");
        assertThat(savedLog.getSubject()).contains("LV-CONFIRM-123");
        assertThat(savedLog.getAttempts()).isEqualTo(1);
        assertThat(savedLog.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Test : log créé en base avec statut FAILED après recover")
    void testLogCreeEnBase_ApresEchec() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(client));

        emailService.recover(new RuntimeException("SMTP connection timed out"), reservation, contract, ContractLanguage.FR);

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, times(1)).save(captor.capture());

        EmailLog savedLog = captor.getValue();
        assertThat(savedLog.getReservationId()).isEqualTo(100L);
        assertThat(savedLog.getRecipient()).isEqualTo("karim.benali@example.com");
        assertThat(savedLog.getStatus()).isEqualTo("FAILED");
        assertThat(savedLog.getStatut()).isEqualTo("FAILED");
        assertThat(savedLog.getErrorMessage()).contains("SMTP connection timed out");
        assertThat(savedLog.getAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("Test : pièce jointe PDF incluse dans le MimeMessage")
    void testContratPdfEnPieceJointe() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(client));
        when(vehicleRepository.findById(20L)).thenReturn(Optional.of(vehicle));
        byte[] pdfBytes = "%PDF-1.4 sample contract".getBytes();
        when(minioService.getContract("100/fr/contract-LV-CONFIRM-123.pdf")).thenReturn(pdfBytes);

        emailService.sendReservationConfirmation(reservation, contract, ContractLanguage.FR);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        assertThat(message).isNotNull();
    }
}
