package ma.locavite.vehicle.dto;

import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    public VehicleFilter withVille(String ville) {
        return new VehicleFilter(marque, categorie, ville, carburant, transmission, brandId, categoryId,
                dateDebut, dateFin, prixMin, prixMax);
    }
}
