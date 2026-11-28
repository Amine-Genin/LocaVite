package ma.locavite.reservation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.locavite.reservation.domain.ReservationOption;

import java.math.BigDecimal;

public record OptionPriceRequest(
        @NotNull(message = "Le type d'option est obligatoire")
        ReservationOption type,

        @NotBlank(message = "Le libellé est obligatoire")
        @Size(max = 120, message = "Le libellé ne doit pas dépasser 120 caractères")
        String libelle,

        @NotNull(message = "Le prix par jour est obligatoire")
        @DecimalMin(value = "0.00", message = "Le prix par jour doit être positif ou nul")
        BigDecimal prixJour,

        Boolean actif
) {}
