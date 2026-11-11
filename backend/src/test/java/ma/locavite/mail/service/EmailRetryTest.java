package ma.locavite.mail.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.auth.service.EmailService;
import ma.locavite.mail.domain.EmailLog;
import ma.locavite.mail.fixtures.EmailTestFixtures;
import ma.locavite.mail.repository.EmailLogRepository;
import ma.locavite.reservation.domain.Reservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.cache.type=none",
        "management.health.mail.enabled=false"
})
class EmailRetryTest {

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private EmailLogRepository emailLogRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Test : retry en cas d'échec effectue 3 tentatives puis enregistre un log FAILED")
    void testRetryEffectueTroisTentativesSurEchec() {
        User user = EmailTestFixtures.createClient(88L, "client.retry@example.com", "Tazi", "Mehdi");
        Reservation reservation = EmailTestFixtures.createReservation(555L, "LV-RETRY-01", user, null);

        when(userRepository.findById(88L)).thenReturn(Optional.of(user));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        doThrow(new MailSendException("SMTP error: connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        emailService.sendReservationConfirmation(reservation);

        verify(mailSender, times(3)).send(any(MimeMessage.class));

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, times(1)).save(captor.capture());

        EmailLog failedLog = captor.getValue();
        assertThat(failedLog.getReservationId()).isEqualTo(555L);
        assertThat(failedLog.getRecipient()).isEqualTo("client.retry@example.com");
        assertThat(failedLog.getStatus()).isEqualTo("FAILED");
        assertThat(failedLog.getStatut()).isEqualTo("FAILED");
        assertThat(failedLog.getTemplateName()).isEqualTo("mail/reservation-confirmation");
        assertThat(failedLog.getAttempts()).isEqualTo(3);
        assertThat(failedLog.getErrorMessage()).contains("SMTP error");
    }
}
