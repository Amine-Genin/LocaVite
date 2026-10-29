package ma.locavite.vehicle.event;

import java.time.LocalDate;

/**
 * Événement publié lors de la confirmation / création d'une nouvelle réservation.
 * Invalide immédiatement le cache de disponibilité du véhicule concerné.
 */
public record ReservationCreatedEvent(
        Long reservationId,
        Long vehicleId,
        LocalDate from,
        LocalDate to
) {}

