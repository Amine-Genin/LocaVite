package ma.locavite.vehicle.dto;

import ma.locavite.vehicle.domain.*;

import java.math.BigDecimal;
import java.time.Instant;

public record VehicleResponse(
        Long id,
        String marque,
        String modele,
        Integer annee,
        CategoryType categorie,
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