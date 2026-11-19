package ma.locavite.sale.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.domain.VehicleCondition;
import ma.locavite.sale.validation.ValidVehicleCondition;

import java.math.BigDecimal;
import java.time.LocalDate;

@ValidVehicleCondition
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
) {
    public String vehicleDetailsValidationError() {
        if (etat == VehicleCondition.NEW) {
            if (kilometrage == null || kilometrage != 0) {
                return "Le kilométrage d'un véhicule neuf doit être égal à 0";
            }
            if (dateMiseEnCirculation != null) {
                return "La date de mise en circulation d'un véhicule neuf doit être absente";
            }
        } else if (etat == VehicleCondition.USED) {
            if (kilometrage == null || kilometrage <= 0) {
                return "Le kilométrage d'un véhicule d'occasion doit être supérieur à 0";
            }
            if (dateMiseEnCirculation == null) {
                return "La date de mise en circulation est obligatoire pour un véhicule d'occasion";
            }
        }
        return null;
    }

    public String vehicleDetailsValidationField() {
        if (etat == VehicleCondition.NEW) {
            return kilometrage == null || kilometrage != 0 ? "kilometrage" : "dateMiseEnCirculation";
        }
        return kilometrage == null || kilometrage <= 0 ? "kilometrage" : "dateMiseEnCirculation";
    }

    public String conditionValidationError() {
        if (etat == null) {
            return "L'état du véhicule est obligatoire (NEW ou USED)";
        }
        String detailsError = vehicleDetailsValidationError();
        if (detailsError != null) {
            return detailsError;
        }
        if (etat == VehicleCondition.NEW && !Integer.valueOf(0).equals(nombreProprietaires)) {
            return "Le nombre de propriétaires d'un véhicule neuf doit être égal à 0";
        }
        if (etat == VehicleCondition.USED && (nombreProprietaires == null || nombreProprietaires < 1)) {
            return "Un véhicule d'occasion doit avoir au moins un propriétaire";
        }
        return null;
    }

    @JsonIgnore
    @AssertTrue(message = "Un véhicule neuf doit avoir 0 propriétaire et une occasion au moins 1")
    public boolean isNombreProprietairesCoherent() {
        if (etat == null) {
            return true;
        }
        if (etat == VehicleCondition.NEW) {
            return Integer.valueOf(0).equals(nombreProprietaires);
        }
        return nombreProprietaires != null && nombreProprietaires >= 1;
    }
}
