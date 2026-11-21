package ma.locavite.sale.dto;

import ma.locavite.sale.domain.VisitStatus;
import ma.locavite.sale.domain.VisitType;

import java.time.Instant;
import java.time.LocalDate;

public record VisitRequestResponse(
        Long id,
        Long saleListingId,
        Long clientId,
        LocalDate dateSouhaitee,
        String creneauHoraire,
        String lieu,
        VisitType typeVisite,
        VisitStatus statut,
        String notes,
        Instant createdAt
) {
}
