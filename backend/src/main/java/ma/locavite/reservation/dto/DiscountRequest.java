package ma.locavite.reservation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.locavite.reservation.domain.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DiscountRequest(
        @NotNull(message = "Le type de remise est obligatoire")
        DiscountType type,

        @Size(max = 60, message = "Le code promo ne doit pas dépasser 60 caractères")
        String code,

        @NotBlank(message = "Le libellé de la remise est obligatoire")
        @Size(max = 150, message = "Le libellé ne doit pas dépasser 150 caractères")
        String libelle,

        @Min(value = 1, message = "Le nombre minimal de jours doit être supérieur à 0")
        Integer minDays,

        @NotNull(message = "Le pourcentage de remise est obligatoire")
        @DecimalMin(value = "0.01", message = "Le pourcentage doit être strictement positif")
        @DecimalMax(value = "100.00", message = "Le pourcentage ne peut pas dépasser 100%")
        BigDecimal pourcentage,

        LocalDate dateDebut,
        LocalDate dateFin,
        Boolean actif
) {}
