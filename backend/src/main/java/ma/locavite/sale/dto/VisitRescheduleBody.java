package ma.locavite.sale.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record VisitRescheduleBody(
        @NotNull @FutureOrPresent(message = "La nouvelle date ne peut pas être passée") LocalDate dateSouhaitee,
        @NotBlank @Size(max = 50) String creneauHoraire,
        @Size(max = 255) String lieu,
        @Size(max = 5000) String notes
) {
}
