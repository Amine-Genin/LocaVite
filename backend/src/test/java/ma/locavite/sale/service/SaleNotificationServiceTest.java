package ma.locavite.sale.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.mail.domain.EmailLog;
import ma.locavite.mail.repository.EmailLogRepository;
import ma.locavite.notification.domain.NotificationChannel;
import ma.locavite.notification.domain.NotificationLog;
import ma.locavite.notification.domain.NotificationPreference;
import ma.locavite.notification.domain.NotificationStatus;
import ma.locavite.notification.repository.NotificationLogRepository;
import ma.locavite.notification.service.SmsService;
import ma.locavite.sale.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SaleNotificationServiceTest {

    private JavaMailSender mailSender;
    private SmsService smsService;
    private UserRepository userRepository;
    private NotificationLogRepository notificationLogRepository;
    private EmailLogRepository emailLogRepository;
    private SaleNotificationService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        smsService = mock(SmsService.class);
        userRepository = mock(UserRepository.class);
        notificationLogRepository = mock(NotificationLogRepository.class);
        emailLogRepository = mock(EmailLogRepository.class);

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        when(smsService.sendSms(anyString(), anyString())).thenReturn(true);

        service = new SaleNotificationService(
                mailSender, smsService, templateEngine, userRepository,
                notificationLogRepository, emailLogRepository
        );
    }

    @Test
    void notifyNewContactRequest_sendsEmailToAgencyAndLogs() {
        SaleListing listing = sampleListing();
        User agency = sampleAgency();
        when(userRepository.findById(listing.getAgenceId())).thenReturn(Optional.of(agency));

        ContactRequest request = new ContactRequest();
        request.setId(10L);
        request.setNom("John Doe");
        request.setEmail("john@example.com");
        request.setTelephone("0611223344");
        request.setMessage("Je suis très intéressé.");
        request.setSaleListing(listing);

        service.notifyNewContactRequest(request, listing);

        verify(mailSender).send(any(MimeMessage.class));
        verify(emailLogRepository).save(any(EmailLog.class));

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        NotificationLog log = captor.getValue();
        assertThat(log.getRecipient()).isEqualTo(agency.getEmail());
        assertThat(log.getNotificationType()).isEqualTo("SALE_CONTACT_REQUEST");
        assertThat(log.getStatus()).isEqualTo(NotificationStatus.SENT.name());
        assertThat(log.getContent()).contains("John Doe");
    }

    @Test
    void notifyNewVisitRequest_sendsEmailAndSmsToAgency() {
        SaleListing listing = sampleListing();
        User agency = sampleAgency();
        when(userRepository.findById(listing.getAgenceId())).thenReturn(Optional.of(agency));

        VisitRequest visit = new VisitRequest();
        visit.setId(20L);
        visit.setTypeVisite(VisitType.ESSAI_ROUTIER);
        visit.setDateSouhaitee(LocalDate.now().plusDays(3));
        visit.setCreneauHoraire("14:00 - 15:00");
        visit.setLieu("Casablanca Centre");
        visit.setSaleListing(listing);

        service.notifyNewVisitRequest(visit, listing);

        verify(mailSender).send(any(MimeMessage.class));
        verify(smsService).sendSms(eq(agency.getTelephone()), contains("ESSAI_ROUTIER"));
        verify(notificationLogRepository, times(2)).save(any(NotificationLog.class));
    }

    @Test
    void notifyVisitConfirmed_sendsEmailAndSmsToClient() {
        SaleListing listing = sampleListing();
        User client = sampleClient();
        when(userRepository.findById(client.getId())).thenReturn(Optional.of(client));

        VisitRequest visit = new VisitRequest();
        visit.setId(21L);
        visit.setClientId(client.getId());
        visit.setTypeVisite(VisitType.VISITE_SIMPLE);
        visit.setDateSouhaitee(LocalDate.now().plusDays(2));
        visit.setCreneauHoraire("10:00");
        visit.setLieu("Agence Rabat");
        visit.setNotes("Apportez votre permis");
        visit.setSaleListing(listing);

        service.notifyVisitConfirmed(visit, listing);

        verify(mailSender).send(any(MimeMessage.class));
        verify(smsService).sendSms(eq(client.getTelephone()), contains("10:00"));
        verify(notificationLogRepository, times(2)).save(any(NotificationLog.class));
    }

    @Test
    void notifyVisitReminderJ1_sendsSmsToBothClientAndAgency() {
        SaleListing listing = sampleListing();
        User client = sampleClient();
        User agency = sampleAgency();
        when(userRepository.findById(client.getId())).thenReturn(Optional.of(client));
        when(userRepository.findById(agency.getId())).thenReturn(Optional.of(agency));

        VisitRequest visit = new VisitRequest();
        visit.setId(22L);
        visit.setClientId(client.getId());
        visit.setDateSouhaitee(LocalDate.now().plusDays(1));
        visit.setCreneauHoraire("11:00");
        visit.setSaleListing(listing);

        service.notifyVisitReminderJ1(visit, listing);

        verify(smsService).sendSms(eq(client.getTelephone()), contains("11:00"));
        verify(smsService).sendSms(eq(agency.getTelephone()), contains("11:00"));
        verify(notificationLogRepository, times(2)).save(any(NotificationLog.class));
    }

    @Test
    void notifyListingPublished_sendsEmailToAgency() {
        SaleListing listing = sampleListing();
        User agency = sampleAgency();
        when(userRepository.findById(agency.getId())).thenReturn(Optional.of(agency));

        service.notifyListingPublished(listing);

        verify(mailSender).send(any(MimeMessage.class));
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getNotificationType()).isEqualTo("SALE_LISTING_PUBLISHED");
    }

    @Test
    void notifyListingSold_sendsEmailToAgency() {
        SaleListing listing = sampleListing();
        User agency = sampleAgency();
        when(userRepository.findById(agency.getId())).thenReturn(Optional.of(agency));

        service.notifyListingSold(listing);

        verify(mailSender).send(any(MimeMessage.class));
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getNotificationType()).isEqualTo("SALE_LISTING_SOLD");
    }

    @Test
    void notifyMessageReceived_sendsEmailToRecipient() {
        SaleListing listing = sampleListing();
        ContactRequest request = new ContactRequest();
        request.setId(12L);
        request.setEmail("client@example.com");
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.empty());

        service.notifyMessageReceived(request, listing, "La voiture est toujours disponible.");

        verify(mailSender).send(any(MimeMessage.class));
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getNotificationType()).isEqualTo("SALE_MESSAGE_RECEIVED");
        assertThat(captor.getValue().getContent()).contains("La voiture est toujours disponible.");
    }

    @Test
    void respectsOptOut_doesNotSendWhenOptedOut() {
        SaleListing listing = sampleListing();
        User agency = User.builder()
                .id(2L)
                .email("agency@example.com")
                .telephone("0600112233")
                .notificationOptIn(false)
                .preferenceNotification(NotificationPreference.NONE)
                .build();
        when(userRepository.findById(listing.getAgenceId())).thenReturn(Optional.of(agency));

        ContactRequest request = new ContactRequest();
        request.setId(10L);
        request.setNom("John");
        request.setEmail("john@example.com");
        request.setTelephone("0611223344");
        request.setMessage("Test opt out");
        request.setSaleListing(listing);

        service.notifyNewContactRequest(request, listing);

        verifyNoInteractions(mailSender);
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.OPTED_OUT.name());
    }

    private SaleListing sampleListing() {
        SaleListing listing = new SaleListing();
        listing.setId(100L);
        listing.setAgenceId(2L);
        listing.setCode("SALE-2026-ABC");
        listing.setTitre("Peugeot 208 GT");
        listing.setPrixVente(BigDecimal.valueOf(180000));
        return listing;
    }

    private User sampleAgency() {
        return User.builder()
                .id(2L)
                .email("agency@locavite.ma")
                .telephone("0600112233")
                .notificationOptIn(true)
                .preferenceNotification(NotificationPreference.EMAIL)
                .build();
    }

    private User sampleClient() {
        return User.builder()
                .id(5L)
                .email("client@locavite.ma")
                .telephone("0699887766")
                .notificationOptIn(true)
                .preferenceNotification(NotificationPreference.EMAIL)
                .build();
    }
}
