package ma.locavite.notification.service;

import ma.locavite.notification.config.TwilioProperties;
import ma.locavite.notification.domain.NotificationLog;
import ma.locavite.notification.domain.NotificationType;
import ma.locavite.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TwilioNotificationMockTest {

    @Mock
    private NotificationLogRepository logRepository;

    private TwilioProperties twilioProperties;
    private TwilioGateway twilioGateway;
    private NotificationTemplateService templateService;
    private SmsServiceImpl smsService;
    private WhatsAppServiceImpl whatsAppService;

    @BeforeEach
    void setUp() {
        twilioProperties = new TwilioProperties();
        twilioProperties.setEnabled(false); // Mode mock dev
        twilioProperties.setAccountSid("mock-account-sid");
        twilioProperties.setAuthToken("mock-auth-token");
        twilioProperties.setPhoneNumber("+212522000000");
        twilioProperties.setWhatsappNumber("whatsapp:+14155238886");

        twilioGateway = new TwilioGateway(twilioProperties);
        templateService = new NotificationTemplateService();

        smsService = new SmsServiceImpl(twilioGateway, logRepository, templateService);
        whatsAppService = new WhatsAppServiceImpl(twilioGateway, logRepository, templateService);
    }

    @Test
    @DisplayName("Test : passerelle Twilio en mode mock (dev) génère un ID fictif sans appel réseau")
    void twilioGatewayInMockModeReturnsMockSid() {
        assertThat(twilioGateway.isTwilioActive()).isFalse();

        String sid = twilioGateway.sendSms("+212611223344", "Message test SMS");
        assertThat(sid).isNotNull().startsWith("MOCK_SMS_");

        String waSid = twilioGateway.sendWhatsApp("+212611223344", "Message test WA");
        assertThat(waSid).isNotNull().startsWith("MOCK_WA_");
    }

    @Test
    @DisplayName("Test : envoi SMS en mode mock enregistre un log SENT en base")
    void sendSmsInMockDevModeSavesSentLog() {
        boolean sent = smsService.sendSms(200L, 10L, "0612345678", "Votre réservation est confirmée", NotificationType.CONFIRMATION);

        assertThat(sent).isTrue();

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(captor.capture());

        NotificationLog logEntry = captor.getValue();
        assertThat(logEntry.getReservationId()).isEqualTo(200L);
        assertThat(logEntry.getUserId()).isEqualTo(10L);
        assertThat(logEntry.getRecipient()).isEqualTo("+212612345678");
        assertThat(logEntry.getChannel()).isEqualTo("SMS");
        assertThat(logEntry.getStatus()).isEqualTo("SENT");
        assertThat(logEntry.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Test : envoi WhatsApp en mode mock enregistre un log SENT en base")
    void sendWhatsAppInMockDevModeSavesSentLog() {
        boolean sent = whatsAppService.sendWhatsApp(201L, 11L, "0699887766", "Votre rappel J-1", NotificationType.REMINDER_J1);

        assertThat(sent).isTrue();

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(captor.capture());

        NotificationLog logEntry = captor.getValue();
        assertThat(logEntry.getReservationId()).isEqualTo(201L);
        assertThat(logEntry.getUserId()).isEqualTo(11L);
        assertThat(logEntry.getRecipient()).isEqualTo("+212699887766");
        assertThat(logEntry.getChannel()).isEqualTo("WHATSAPP");
        assertThat(logEntry.getStatus()).isEqualTo("SENT");
    }
}
