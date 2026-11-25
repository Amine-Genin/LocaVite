package ma.locavite.notification.domain;

import java.util.Locale;

public enum NotificationType {
    CONFIRMATION,
    REMINDER_J1,
    COMPLETION,
    CANCELLATION,
    REFUND,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    CASH_PAYMENT_CONFIRMED,
    SALE_CONTACT_REQUEST,
    SALE_VISIT_REQUEST,
    SALE_VISIT_CONFIRMED,
    SALE_VISIT_REMINDER_J1,
    SALE_LISTING_PUBLISHED,
    SALE_LISTING_SOLD,
    SALE_MESSAGE_RECEIVED;

    public static NotificationType from(String value) {
        if (value == null || value.isBlank()) {
            return CONFIRMATION;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return CONFIRMATION;
        }
    }
}
