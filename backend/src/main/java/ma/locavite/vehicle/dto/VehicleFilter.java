package ma.locavite.vehicle.dto;

import ma.locavite.vehicle.domain.CategoryType;
import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;

/** Filtres de recherche : ?marque=Toyota&categorie=SUV&ville=Casablanca */
public record VehicleFilter(
        String marque,
        CategoryType categorie,
        String ville,
        FuelType carburant,
        TransmissionType transmission
) {}