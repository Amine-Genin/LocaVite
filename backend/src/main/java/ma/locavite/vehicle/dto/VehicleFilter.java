package ma.locavite.vehicle.dto;

import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;

/** Filtres de recherche par identifiants de catalogue ou par noms. */
public record VehicleFilter(
        String marque,
        String categorie,
        String ville,
        FuelType carburant,
        TransmissionType transmission,
        Long brandId,
        Long categoryId
) {}