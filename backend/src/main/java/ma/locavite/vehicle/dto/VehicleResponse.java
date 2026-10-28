package ma.locavite.vehicle.dto;

import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;
import ma.locavite.vehicle.domain.VehicleStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record VehicleResponse(
        Long id,
        Long brandId,
        String marque,
        String modele,
        Integer annee,
        Long categoryId,
        String categorie,
        String ville,
        BigDecimal prixJour,
        BigDecimal prixVente,
        FuelType carburant,
        TransmissionType transmission,
        Integer places,
        String description,
        String matricule,
        VehicleStatus statut,
        Long agenceId,
        Instant createdAt,
        Instant updatedAt
) {}