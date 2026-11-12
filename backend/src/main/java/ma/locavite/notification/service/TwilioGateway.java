package ma.locavite.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import ma.locavite.notification.config.TwilioProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TwilioGateway {

    private static final Logger log = LoggerFactory.getLogger(TwilioGateway.class);

    private final TwilioProperties twilioProperties;
    private boolean initialized = false;

    public TwilioGateway(TwilioProperties twilioProperties) {
        this.twilioProperties = twilioProperties;
    }

    public String sendSms(String to, String body) {
        String from = twilioProperties.getPhoneNumber();
        if (!isTwilioActive()) {
            String mockSid = "MOCK_SMS_" + UUID.randomUUID();
            log.info("[MOCK SMS] to={}, from={}, sid={}, body={}", to, from, mockSid, body);
            return mockSid;
        }

        ensureInitialized();
        Message message = Message.creator(new PhoneNumber(to), new PhoneNumber(from), body).create();
        log.info("Twilio SMS sent to={}, sid={}", to, message.getSid());
        return message.getSid();
    }

    public String sendWhatsApp(String to, String body) {
        String from = twilioProperties.getWhatsappNumber();
        String toFormatted = to.startsWith("whatsapp:") ? to : "whatsapp:" + to;
        String fromFormatted = from.startsWith("whatsapp:") ? from : "whatsapp:" + from;

        if (!isTwilioActive()) {
            String mockSid = "MOCK_WA_" + UUID.randomUUID();
            log.info("[MOCK WHATSAPP] to={}, from={}, sid={}, body={}", toFormatted, fromFormatted, mockSid, body);
            return mockSid;
        }

        ensureInitialized();
        Message message = Message.creator(
                new PhoneNumber(toFormatted),
                new PhoneNumber(fromFormatted),
                body
        ).create();
        log.info("Twilio WhatsApp sent to={}, sid={}", toFormatted, message.getSid());
        return message.getSid();
    }

    public boolean isTwilioActive() {
        return twilioProperties.isEnabled()
                && twilioProperties.getAccountSid() != null
                && !twilioProperties.getAccountSid().isBlank()
                && !twilioProperties.getAccountSid().toLowerCase().contains("mock");
    }

    private synchronized void ensureInitialized() {
        if (!initialized) {
            Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
            initialized = true;
        }
    }
}
