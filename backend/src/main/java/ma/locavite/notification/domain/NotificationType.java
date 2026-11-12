package ma.locavite.notification.domain;

import java.util.Locale;

public enum NotificationType {
    CONFIRMATION,
    REMINDER_J1,
    COMPLETION,
    CANCELLATION;

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
