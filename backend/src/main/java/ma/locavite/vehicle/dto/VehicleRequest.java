package ma.locavite.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import ma.locavite.vehicle.domain.*;

import java.math.BigDecimal;
import java.time.Year;

public record VehicleRequest(
        @NotBlank @Size(max = 100) String marque,
        @NotBlank @Size(max = 100) String modele,
        @NotNull @Min(value = 1990, message = "L'année doit être >= 1990") Integer annee,
        @NotNull CategoryType categorie,
        @NotBlank @Size(max = 100) String ville,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "Le prix par jour doit être > 0") BigDecimal prixJour,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "Le prix de vente doit être > 0") BigDecimal prixVente,
        @NotNull FuelType carburant,
        @NotNull TransmissionType transmission,
        @NotNull @Positive Integer places,
        @NotBlank String description,
        @NotBlank @Size(max = 30) String matricule,
        VehicleStatus statut // optionnel : DRAFT par défaut à la création
) {

    /** Borne haute dynamique : année courante + 1. */
    @JsonIgnore
    @AssertTrue(message = "L'année doit être comprise entre 1990 et l'année en cours + 1")
    public boolean isAnneeValid() {
        return annee == null || annee <= Year.now().getValue() + 1;
    }
}