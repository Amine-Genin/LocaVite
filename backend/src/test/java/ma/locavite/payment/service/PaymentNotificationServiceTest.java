package ma.locavite.payment.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.mail.domain.EmailLog;
import ma.locavite.mail.repository.EmailLogRepository;
import ma.locavite.notification.domain.NotificationPreference;
import ma.locavite.notification.repository.NotificationLogRepository;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.event.PaymentFailedEvent;
import ma.locavite.reservation.config.ContractProperties;
import ma.locavite.reservation.repository.ReservationContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Properties;

import static org.mockito.Mockito.*;

class PaymentNotificationServiceTest {
    private JavaMailSender mailSender;
    private UserRepository userRepository;
    private EmailLogRepository emailLogs;
    private PaymentNotificationService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        userRepository = mock(UserRepository.class);
        emailLogs = mock(EmailLogRepository.class);
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        TemplateEngine templates = new org.thymeleaf.spring6.SpringTemplateEngine();
        ((org.thymeleaf.spring6.SpringTemplateEngine) templates).setTemplateResolver(resolver);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        service = new PaymentNotificationService(mailSender, emailLogs, userRepository, templates,
                mock(CashReceiptPdfService.class), mock(ReservationContractRepository.class),
                new ContractProperties(), mock(NotificationLogRepository.class));
    }

    @Test
    void sendsLocalizedFailureEmailAndCreatesLog() {
        User user = User.builder().id(7L).email("client@example.com").telephone("0611223344")
                .preferenceNotification(NotificationPreference.SMS).notificationOptIn(true).build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        Payment payment = payment(PaymentStatus.FAILED);

        service.onPaymentFailed(new PaymentFailedEvent(payment));

        verify(mailSender).send(any(MimeMessage.class));
        verify(emailLogs).save(any(EmailLog.class));
    }

    @Test
    void doesNotSendEmailWhenUserOptedOut() {
        User user = User.builder().id(7L).email("client@example.com").telephone("0611223344")
                .preferenceNotification(NotificationPreference.NONE).notificationOptIn(false).build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        service.onPaymentFailed(new PaymentFailedEvent(payment(PaymentStatus.FAILED)));

        verifyNoInteractions(mailSender);
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(8L);
        payment.setUserId(7L);
        payment.setReservationId(9L);
        payment.setCode("PAY-2026-8");
        payment.setMontant(new BigDecimal("100.00"));
        payment.setDevise("MAD");
        payment.setStatut(status);
        return payment;
    }
}
