package ma.locavite.vehicle.dto;
import ma.locavite.vehicle.domain.VehicleStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
public record FleetVehicleResponse(Long id, String matricule, String marque, String modele, Integer annee,
        String categorie, VehicleStatus statut, String ville, String localisationActuelle,
        Long prochaineReservationId, LocalDate prochaineReservation, Integer kilometrage,
        LocalDate derniereMaintenance, LocalDate prochaineMaintenance, BigDecimal revenusMois,
        BigDecimal revenusAnnee, double tauxOccupation, Long agenceId, Instant createdAt) {}
