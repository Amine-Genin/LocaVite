package ma.locavite.vehicle.service;

import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.dto.AvailabilityResponse;
import ma.locavite.vehicle.dto.Period;
import ma.locavite.vehicle.dto.UnavailablePeriod;
import ma.locavite.vehicle.event.ReservationCreatedEvent;
import ma.locavite.vehicle.exception.VehicleNotFoundException;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AvailabilityService {

    private final VehicleRepository vehicleRepository;
    private final ReservationProvider reservationProvider;

    public AvailabilityService(VehicleRepository vehicleRepository, ReservationProvider reservationProvider) {
        this.vehicleRepository = vehicleRepository;
        this.reservationProvider = reservationProvider;
    }

    /**
     * Retourne les périodes disponibles et indisponibles d'un véhicule sur l'intervalle [from, to].
     * Mis en cache pendant 1 minute.
     */
    @Cacheable(value = "vehicleAvailability", key = "#vehicleId + '-' + #from + '-' + #to")
    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(Long vehicleId, LocalDate from, LocalDate to) {
        validateDates(from, to);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .filter(v -> v.getStatut() != VehicleStatus.ARCHIVED)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        List<UnavailablePeriod> unavailable = reservationProvider.getUnavailablePeriods(vehicle.getId(), from, to);
        return calculateAvailability(from, to, unavailable);
    }

    /**
     * Invalide immédiatement le cache de disponibilité lors de la création d'une réservation.
     */
    @EventListener
    @CacheEvict(value = "vehicleAvailability", allEntries = true)
    public void onReservationCreated(ReservationCreatedEvent event) {
        // Le cache est invalidé dès confirmation d'une réservation
    }

    /**
     * Invalidation manuelle du cache de disponibilité.
     */
    @CacheEvict(value = "vehicleAvailability", allEntries = true)
    public void invalidateCache() {
        // Utilisé pour maintenance ou synchronisation
    }

    private static void validateDates(LocalDate from, LocalDate to) {
        if (from == null) {
            throw new IllegalArgumentException("La date de début 'from' est obligatoire");
        }
        if (to == null) {
            throw new IllegalArgumentException("La date de fin 'to' est obligatoire");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La date de fin 'to' doit être postérieure ou égale à la date de début 'from'");
        }
    }

    public AvailabilityResponse calculateAvailability(LocalDate from, LocalDate to, List<UnavailablePeriod> rawUnavailable) {
        if (rawUnavailable == null || rawUnavailable.isEmpty()) {
            return new AvailabilityResponse(List.of(new Period(from, to)), List.of());
        }

        // 1. Filtrer et borner les périodes indisponibles dans l'intervalle [from, to]
        List<UnavailablePeriod> filtered = rawUnavailable.stream()
                .filter(u -> !u.to().isBefore(from) && !u.from().isAfter(to))
                .map(u -> {
                    LocalDate start = u.from().isBefore(from) ? from : u.from();
                    LocalDate end = u.to().isAfter(to) ? to : u.to();
                    return new UnavailablePeriod(start, end, u.reason() != null ? u.reason() : "RESERVATION");
                })
                .sorted(Comparator.comparing(UnavailablePeriod::from).thenComparing(UnavailablePeriod::to))
                .toList();

        if (filtered.isEmpty()) {
            return new AvailabilityResponse(List.of(new Period(from, to)), List.of());
        }

        // 2. Fusionner les périodes indisponibles qui se chevauchent ou sont contiguës
        List<UnavailablePeriod> mergedUnavailable = new ArrayList<>();
        UnavailablePeriod current = filtered.get(0);
        for (int i = 1; i < filtered.size(); i++) {
            UnavailablePeriod next = filtered.get(i);
            if (!next.from().isAfter(current.to().plusDays(1))) {
                LocalDate maxEnd = current.to().isAfter(next.to()) ? current.to() : next.to();
                current = new UnavailablePeriod(current.from(), maxEnd, current.reason());
            } else {
                mergedUnavailable.add(current);
                current = next;
            }
        }
        mergedUnavailable.add(current);

        // 3. Déduire les plages disponibles
        List<Period> available = new ArrayList<>();
        LocalDate cursor = from;
        for (UnavailablePeriod u : mergedUnavailable) {
            if (cursor.isBefore(u.from())) {
                available.add(new Period(cursor, u.from().minusDays(1)));
            }
            if (!cursor.isAfter(u.to())) {
                cursor = u.to().plusDays(1);
            }
        }
        if (!cursor.isAfter(to)) {
            available.add(new Period(cursor, to));
        }

        return new AvailabilityResponse(available, mergedUnavailable);
    }
}

