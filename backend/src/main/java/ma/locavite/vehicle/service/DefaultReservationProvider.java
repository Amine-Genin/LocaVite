package ma.locavite.vehicle.service;

import ma.locavite.vehicle.dto.UnavailablePeriod;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Pour Sprint 2 : mock retournant aucune indisponibilité (le véhicule est toujours disponible).
 * Sera complété / substitué au Sprint 3 lors du branchement du module Réservation.
 */
@Component
public class DefaultReservationProvider implements ReservationProvider {

    @Override
    public List<UnavailablePeriod> getUnavailablePeriods(Long vehicleId, LocalDate from, LocalDate to) {
        return Collections.emptyList();
    }
}
