package ma.locavite.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ma.locavite.reservation.domain.ReservationStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ReservationFilter(
        @Schema(description = "Filtrer par identifiant utilisateur")
        Long userId,

        @Schema(description = "Filtrer par identifiant véhicule")
        Long vehicleId,

        @Schema(description = "Filtrer par statut de réservation")
        ReservationStatus statut,

        @Schema(description = "Filtrer par code de réservation")
        String code,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(description = "Date début minimale")
        LocalDate dateDebut,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(description = "Date fin maximale")
        LocalDate dateFin
) {}
