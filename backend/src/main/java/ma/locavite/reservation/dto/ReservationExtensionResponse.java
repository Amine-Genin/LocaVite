package ma.locavite.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReservationExtensionResponse(
        @Schema(description = "Identifiant unique de la prolongation", example = "1")
        Long id,

        @Schema(description = "Identifiant de la réservation", example = "42")
        Long reservationId,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Ancienne date de fin", example = "2026-11-15")
        LocalDate ancienneDateFin,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Nouvelle date de fin", example = "2026-11-18")
        LocalDate nouvelleDateFin,

        @Schema(description = "Prix supplémentaire facturé pour la prolongation", example = "1200.00")
        BigDecimal prixSupplementaire,

        @Schema(description = "Date et heure de la prolongation")
        Instant createdAt
) {}
