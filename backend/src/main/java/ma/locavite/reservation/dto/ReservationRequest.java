package ma.locavite.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ma.locavite.reservation.domain.ReservationOption;

import java.time.LocalDate;
import java.util.Set;

public record ReservationRequest(
        @NotNull(message = "L'ID du véhicule est obligatoire")
        @Schema(description = "Identifiant du véhicule", example = "1")
        Long vehicleId,

        @NotNull(message = "La date de début est obligatoire")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Date de début de réservation (format YYYY-MM-DD)", example = "2026-11-10")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Date de fin de réservation (format YYYY-MM-DD)", example = "2026-11-15")
        LocalDate dateFin,

        @NotBlank(message = "Le lieu de prise en charge est obligatoire")
        @Schema(description = "Lieu de prise en charge du véhicule", example = "Aéroport Mohammed V, Casablanca")
        String lieuPriseEnCharge,

        @NotBlank(message = "Le lieu de retour est obligatoire")
        @Schema(description = "Lieu de retour du véhicule", example = "Aéroport Mohammed V, Casablanca")
        String lieuRetour,

        @Schema(description = "Options sélectionnées (GPS, BABY_SEAT, EXTRA_DRIVER)")
        Set<ReservationOption> options
) {}
