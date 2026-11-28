package ma.locavite.vehicle.dto;
import java.time.Instant;
import java.time.LocalDate;
public record FleetHistoryResponse(String type, String description, Instant dateHeure, LocalDate date,
        String ancienStatut, String nouveauStatut, Integer kilometrage, Long referenceId) {}
