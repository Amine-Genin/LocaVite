package ma.locavite.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ma.locavite.reservation.domain.ReservationOption;

import java.time.LocalDate;
import java.util.Set;

public record QuoteRequest(
        @NotNull
        @Schema(description = "Identifiant du véhicule", example = "1")
        Long vehicleId,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Date de début (incluse)", example = "2026-11-10")
        LocalDate dateDebut,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Date de fin (exclue)", example = "2026-11-15")
        LocalDate dateFin,

        @Schema(description = "Options sélectionnées")
        Set<ReservationOption> options
) {}
