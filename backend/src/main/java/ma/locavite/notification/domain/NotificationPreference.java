package ma.locavite.notification.domain;

import java.util.Locale;

public enum NotificationPreference {
    SMS,
    WHATSAPP,
    EMAIL,
    NONE;

    public static NotificationPreference from(String value) {
        if (value == null || value.isBlank()) {
            return SMS;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SMS;
        }
    }
}
