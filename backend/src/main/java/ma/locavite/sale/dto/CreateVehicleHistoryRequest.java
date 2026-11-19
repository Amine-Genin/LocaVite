package ma.locavite.sale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ma.locavite.sale.domain.HistoryEventType;

import java.time.LocalDate;

public record CreateVehicleHistoryRequest(
        @NotNull HistoryEventType typeEvenement,
        @NotNull LocalDate date,
        @NotBlank @Size(max = 2000) String description,
        @NotNull @PositiveOrZero Integer kilometrage
) {}
