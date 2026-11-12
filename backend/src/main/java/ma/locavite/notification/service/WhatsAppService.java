package ma.locavite.notification.service;

import ma.locavite.notification.domain.NotificationType;

public interface WhatsAppService {

    boolean sendWhatsApp(String toPhoneNumber, String message);

    boolean sendWhatsApp(Long reservationId, Long userId, String toPhoneNumber, String message);

    boolean sendWhatsApp(Long reservationId, Long userId, String toPhoneNumber, String message, NotificationType type);
}
