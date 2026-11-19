package ma.locavite.sale.dto;

import ma.locavite.sale.domain.HistoryEventType;

import java.time.LocalDate;

public record VehicleHistoryResponse(
        Long id,
        Long saleListingId,
        Long vehicleId,
        HistoryEventType typeEvenement,
        LocalDate date,
        String description,
        Integer kilometrage,
        String documentUrl,
        boolean verified
) {}
