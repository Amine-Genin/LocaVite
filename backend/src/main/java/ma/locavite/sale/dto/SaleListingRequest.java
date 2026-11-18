package ma.locavite.sale.dto;

import jakarta.validation.constraints.*;
import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.domain.VehicleCondition;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaleListingRequest(
        @NotNull(message = "L'identifiant du véhicule est obligatoire")
        @Positive(message = "L'identifiant du véhicule doit être positif")
        Long vehicleId,

        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 255, message = "Le titre ne doit pas dépasser 255 caractères")
        String titre,

        @NotBlank(message = "La description détaillée est obligatoire")
        String description,

        @NotNull(message = "Le prix de vente est obligatoire")
        @DecimalMin(value = "0.0", inclusive = false, message = "Le prix de vente doit être strictement supérieur à 0")
        BigDecimal prixVente,

        Boolean prixNego,

        @NotNull(message = "L'état du véhicule est obligatoire (NEW ou USED)")
        VehicleCondition etat,

        @PositiveOrZero(message = "Le kilométrage doit être positif ou nul")
        Integer kilometrage,

        Boolean premierProprietaire,

        LocalDate dateMiseEnCirculation,

        @Size(max = 50, message = "Le numéro de châssis (VIN) ne doit pas dépasser 50 caractères")
        String numeroChassis,

        @PositiveOrZero(message = "Le nombre de propriétaires doit être positif ou nul")
        Integer nombreProprietaires,

        SaleStatus statut
) {}
