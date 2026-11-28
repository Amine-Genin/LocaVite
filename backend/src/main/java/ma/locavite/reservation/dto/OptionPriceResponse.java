package ma.locavite.reservation.dto;

import ma.locavite.reservation.domain.OptionPrice;
import ma.locavite.reservation.domain.ReservationOption;

import java.math.BigDecimal;

public record OptionPriceResponse(
        Long id,
        ReservationOption type,
        String libelle,
        BigDecimal prixJour,
        Boolean actif
) {
    public static OptionPriceResponse fromEntity(OptionPrice entity) {
        return new OptionPriceResponse(
                entity.getId(),
                entity.getType(),
                entity.getLibelle(),
                entity.getPrixJour(),
                entity.getActif()
        );
    }
}
