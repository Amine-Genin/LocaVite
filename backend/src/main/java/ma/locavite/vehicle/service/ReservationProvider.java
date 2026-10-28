package ma.locavite.vehicle.service;

import ma.locavite.vehicle.dto.UnavailablePeriod;

import java.time.LocalDate;
import java.util.List;

/**
 * Fournisseur des périodes indisponibles (réservations confirmées/actives, maintenances).
 * Intégré au module de réservation au Sprint 3.
 */
public interface ReservationProvider {
    List<UnavailablePeriod> getUnavailablePeriods(Long vehicleId, LocalDate from, LocalDate to);
}

