package ma.locavite.vehicle.dto;

import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Filtres de recherche par identifiants de catalogue ou par noms. */
public record VehicleFilter(
        String marque,
        String categorie,
        String ville,
        FuelType carburant,
        TransmissionType transmission,
        Long brandId,
        Long categoryId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateFin,
        BigDecimal prixMin,
        BigDecimal prixMax
) {
    public String cacheKey(org.springframework.data.domain.Pageable pageable) {
        if (pageable == null) {
            return Integer.toHexString(hashCode());
        }
        return Integer.toHexString(Objects.hash(this, pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().toString()));
    }

    public VehicleFilter withVille(String ville) {
        return new VehicleFilter(marque, categorie, ville, carburant, transmission, brandId, categoryId,
                dateDebut, dateFin, prixMin, prixMax);
    }
}
