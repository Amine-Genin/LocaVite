package ma.locavite.notification.service;

import ma.locavite.notification.domain.NotificationType;

public interface SmsService {

    boolean sendSms(String toPhoneNumber, String message);

    boolean sendSms(Long reservationId, Long userId, String toPhoneNumber, String message);

    boolean sendSms(Long reservationId, Long userId, String toPhoneNumber, String message, NotificationType type);
}
