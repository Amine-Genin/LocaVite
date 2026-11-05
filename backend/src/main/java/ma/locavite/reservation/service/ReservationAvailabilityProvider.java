package ma.locavite.reservation.service;

import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.dto.UnavailablePeriod;
import ma.locavite.vehicle.service.ReservationProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Primary
public class ReservationAvailabilityProvider implements ReservationProvider {

    private final ReservationRepository reservationRepository;

    public ReservationAvailabilityProvider(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public List<UnavailablePeriod> getUnavailablePeriods(Long vehicleId, LocalDate from, LocalDate to) {
        List<ReservationStatus> activeStatuses = List.of(
                ReservationStatus.PENDING,
                ReservationStatus.CONFIRMED,
                ReservationStatus.ACTIVE
        );
        List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
                vehicleId,
                from,
                to,
                activeStatuses,
                null
        );

        return overlapping.stream()
                .map(r -> new UnavailablePeriod(r.getDateDebut(), r.getDateFin(), "RESERVATION"))
                .toList();
    }
}
