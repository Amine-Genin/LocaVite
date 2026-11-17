package ma.locavite.payment.cmi.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class CmiDataSanitizer {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "secretkey",
            "storekey",
            "store_key",
            "secret_key",
            "pan",
            "card_number",
            "cardnumber",
            "cvv",
            "cvc",
            "cavv",
            "expiry",
            "expdate",
            "password"
    );

    private CmiDataSanitizer() {
    }

    public static String maskPan(String pan) {
        if (pan == null || pan.isBlank()) {
            return "";
        }
        String clean = pan.replaceAll("\\s|-", "");
        if (clean.length() < 10) {
            return "****";
        }
        String first6 = clean.substring(0, 6);
        String last4 = clean.substring(clean.length() - 4);
        int middleCount = Math.max(clean.length() - 10, 4);
        return first6 + "*".repeat(middleCount) + last4;
    }

    public static Map<String, String> maskSensitiveParams(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> sanitized = new HashMap<>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                sanitized.put(key, null);
                continue;
            }
            String strValue = String.valueOf(value);
            String lowerKey = key.toLowerCase();
            if (SENSITIVE_KEYS.contains(lowerKey)) {
                if (lowerKey.contains("pan") || lowerKey.contains("card")) {
                    sanitized.put(key, maskPan(strValue));
                } else {
                    sanitized.put(key, "******");
                }
            } else {
                sanitized.put(key, strValue);
            }
        }
        return sanitized;
    }

    public static String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String sanitized = value.replaceAll(
                "(?i)(\"?(?:pan|cardNumber|card_number|cvv|cvc|cavv|expiry|expDate)\"?\\s*[:=]\\s*\"?)[^\",}\\s]+",
                "$1[REDACTED]"
        );
        return sanitized.replaceAll("\\b\\d{13,19}\\b", "[REDACTED]");
    }
}
