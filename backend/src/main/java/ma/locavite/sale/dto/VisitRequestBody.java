package ma.locavite.sale.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.locavite.sale.domain.VisitType;

import java.time.LocalDate;

public record VisitRequestBody(
        @NotNull @FutureOrPresent(message = "La date souhaitée ne peut pas être passée") LocalDate dateSouhaitee,
        @NotBlank @Size(max = 50) String creneauHoraire,
        @NotBlank @Size(max = 255) String lieu,
        @NotNull VisitType typeVisite
) {
}
