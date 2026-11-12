package ma.locavite.notification.service;

import ma.locavite.notification.domain.NotificationLog;
import ma.locavite.notification.domain.NotificationType;
import ma.locavite.notification.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppServiceImpl.class);

    private final TwilioGateway twilioGateway;
    private final NotificationLogRepository logRepository;
    private final NotificationTemplateService templateService;

    public WhatsAppServiceImpl(TwilioGateway twilioGateway,
                               NotificationLogRepository logRepository,
                               NotificationTemplateService templateService) {
        this.twilioGateway = twilioGateway;
        this.logRepository = logRepository;
        this.templateService = templateService;
    }

    @Override
    public boolean sendWhatsApp(String toPhoneNumber, String message) {
        return sendWhatsApp(null, null, toPhoneNumber, message, NotificationType.CONFIRMATION);
    }

    @Override
    public boolean sendWhatsApp(Long reservationId, Long userId, String toPhoneNumber, String message) {
        return sendWhatsApp(reservationId, userId, toPhoneNumber, message, NotificationType.CONFIRMATION);
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public boolean sendWhatsApp(Long reservationId, Long userId, String toPhoneNumber, String message, NotificationType type) {
        String formattedNumber = templateService.formatMoroccanPhoneNumber(toPhoneNumber);
        String notifType = (type != null) ? type.name() : NotificationType.CONFIRMATION.name();

        twilioGateway.sendWhatsApp(formattedNumber, message);

        int attempt = 1;
        if (RetrySynchronizationManager.getContext() != null) {
            attempt = RetrySynchronizationManager.getContext().getRetryCount() + 1;
        }

        saveLog(reservationId, userId, formattedNumber, "WHATSAPP", notifType, message, "SENT", null, attempt);
        log.info("WhatsApp envoyé avec succès à {} [reservationId={}, tentative={}]", formattedNumber, reservationId, attempt);
        return true;
    }

    @Recover
    public boolean recover(Exception exception, Long reservationId, Long userId, String toPhoneNumber, String message, NotificationType type) {
        String formattedNumber = templateService.formatMoroccanPhoneNumber(toPhoneNumber);
        String notifType = (type != null) ? type.name() : NotificationType.CONFIRMATION.name();
        log.error("Échec définitif de l'envoi WhatsApp à {} après 3 tentatives [reservationId={}] : {}",
                formattedNumber, reservationId, exception.getMessage());
        saveLog(reservationId, userId, formattedNumber, "WHATSAPP", notifType, message, "FAILED", exception.getMessage(), 3);
        return false;
    }

    private void saveLog(Long reservationId, Long userId, String recipient, String channel,
                         String notificationType, String content, String status,
                         String errorMessage, int attempts) {
        if (logRepository == null) {
            return;
        }
        try {
            NotificationLog logEntry = new NotificationLog(
                    reservationId,
                    userId,
                    recipient,
                    channel,
                    notificationType,
                    content,
                    status,
                    errorMessage,
                    attempts
            );
            logRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Impossible d'enregistrer le log de notification WhatsApp : {}", e.getMessage());
        }
    }
}
