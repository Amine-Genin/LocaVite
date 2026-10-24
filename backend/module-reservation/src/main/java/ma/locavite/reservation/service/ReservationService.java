package ma.locavite.reservation.service;

import lombok.extern.slf4j.Slf4j;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class ReservationService {

    private final ConcurrentHashMap<Long, Reservation> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public List<Reservation> getReservationsByUser(Long userId) {
        return storage.values().stream()
                .filter(r -> userId.equals(r.getUserId()))
                .toList();
    }

    public Optional<Reservation> getReservationById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Reservation creerReservation(Reservation reservation) {
        if (reservation.getId() == null) {
            reservation.setId(idGenerator.getAndIncrement());
        }
        if (reservation.getStatut() == null) {
            reservation.setStatut(ReservationStatus.PENDING);
        }
        storage.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation annulerReservation(Long id) {
        Reservation reservation = storage.get(id);
        if (reservation == null) {
            throw new IllegalArgumentException("Réservation introuvable avec l'id : " + id);
        }
        reservation.setStatut(ReservationStatus.CANCELLED);
        return reservation;
    }
}
