package ma.locavite.notification.domain;

import java.util.Locale;

public enum NotificationStatus {
    SENT,
    FAILED,
    OPTED_OUT,
    SKIPPED,
    PENDING;

    public static NotificationStatus from(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}
