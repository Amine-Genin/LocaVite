package ma.locavite.notification.service;

import ma.locavite.notification.domain.NotificationLog;
import ma.locavite.notification.domain.NotificationType;
import ma.locavite.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.cache.type=none",
        "management.health.mail.enabled=false"
})
class NotificationRetryTest {

    @Autowired
    private SmsService smsService;

    @Autowired
    private WhatsAppService whatsAppService;

    @MockBean
    private TwilioGateway twilioGateway;

    @MockBean
    private NotificationLogRepository logRepository;

    @Test
    @DisplayName("Test retry SMS : effectue 3 tentatives sur échec réseau puis enregistre un log FAILED")
    void testSmsRetryThreeTimesOnFailure() {
        when(twilioGateway.sendSms(anyString(), anyString()))
                .thenThrow(new RuntimeException("Twilio service temporarily unavailable"));

        boolean result = smsService.sendSms(300L, 1L, "+212611223344", "Message test", NotificationType.CONFIRMATION);

        assertThat(result).isFalse();
        verify(twilioGateway, times(3)).sendSms(anyString(), anyString());

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(1)).save(captor.capture());

        NotificationLog failedLog = captor.getValue();
        assertThat(failedLog.getReservationId()).isEqualTo(300L);
        assertThat(failedLog.getRecipient()).isEqualTo("+212611223344");
        assertThat(failedLog.getChannel()).isEqualTo("SMS");
        assertThat(failedLog.getStatus()).isEqualTo("FAILED");
        assertThat(failedLog.getAttempts()).isEqualTo(3);
        assertThat(failedLog.getErrorMessage()).contains("Twilio service temporarily unavailable");
    }

    @Test
    @DisplayName("Test retry WhatsApp : effectue 3 tentatives sur échec puis enregistre un log FAILED")
    void testWhatsAppRetryThreeTimesOnFailure() {
        when(twilioGateway.sendWhatsApp(anyString(), anyString()))
                .thenThrow(new RuntimeException("WhatsApp API gateway timeout"));

        boolean result = whatsAppService.sendWhatsApp(301L, 2L, "+212611223344", "Message test WA", NotificationType.REMINDER_J1);

        assertThat(result).isFalse();
        verify(twilioGateway, times(3)).sendWhatsApp(anyString(), anyString());

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(1)).save(captor.capture());

        NotificationLog failedLog = captor.getValue();
        assertThat(failedLog.getReservationId()).isEqualTo(301L);
        assertThat(failedLog.getChannel()).isEqualTo("WHATSAPP");
        assertThat(failedLog.getStatus()).isEqualTo("FAILED");
        assertThat(failedLog.getAttempts()).isEqualTo(3);
        assertThat(failedLog.getErrorMessage()).contains("WhatsApp API gateway timeout");
    }
}
