package ma.locavite.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExtendReservationRequest(
        @NotNull(message = "La nouvelle date de fin est obligatoire")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Nouvelle date de fin souhaitée", example = "2026-11-20")
        LocalDate newDateFin,

        @NotNull(message = "L'acceptation du nouveau prix est obligatoire")
        @Schema(description = "Acceptation explicite du nouveau prix recalculé", example = "true")
        Boolean acceptNewPrice
) {}
