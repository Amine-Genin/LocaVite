package ma.locavite.reservation.event;

import ma.locavite.reservation.domain.Reservation;

public record ReservationCompletedEvent(Reservation reservation) {
}
