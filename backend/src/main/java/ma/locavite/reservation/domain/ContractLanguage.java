package ma.locavite.reservation.domain;

import java.util.Locale;

public enum ContractLanguage {
    FR,
    AR,
    EN;

    public static ContractLanguage from(String value) {
        if (value == null || value.isBlank()) {
            return FR;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("La langue doit être FR, AR ou EN");
        }
    }
}
