package ma.locavite.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import ma.locavite.reservation.domain.ReservationOption;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.domain.CautionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record ReservationResponse(
        @Schema(description = "Identifiant unique de la réservation", example = "1")
        Long id,

        @Schema(description = "Code unique de réservation au format LV-XXXXXX", example = "LV-A1B2C3")
        String code,

        @Schema(description = "Identifiant de l'utilisateur ayant réservé", example = "42")
        Long userId,

        @Schema(description = "Identifiant du véhicule réservé", example = "10")
        Long vehicleId,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Date de début", example = "2026-11-10")
        LocalDate dateDebut,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Date de fin", example = "2026-11-15")
        LocalDate dateFin,

        @Schema(description = "Lieu de prise en charge", example = "Aéroport Mohammed V, Casablanca")
        String lieuPriseEnCharge,

        @Schema(description = "Lieu de retour", example = "Aéroport Mohammed V, Casablanca")
        String lieuRetour,

        @Schema(description = "Prix total de la réservation", example = "1500.00")
        BigDecimal prixTotal,

        @Schema(description = "Montant de la caution", example = "3000.00")
        BigDecimal caution,

        @Schema(description = "Statut de la caution", example = "PENDING")
        CautionStatus cautionStatut,

        @Schema(description = "Statut de la réservation", example = "PENDING")
        ReservationStatus statut,

        @Schema(description = "Options de la réservation")
        Set<ReservationOption> options,

        @Schema(description = "Historique des prolongations de la réservation")
        List<ReservationExtensionResponse> extensions,

        @Schema(description = "Date et heure de création")
        Instant createdAt,

        @Schema(description = "Date et heure de dernière mise à jour")
        Instant updatedAt
) {}
