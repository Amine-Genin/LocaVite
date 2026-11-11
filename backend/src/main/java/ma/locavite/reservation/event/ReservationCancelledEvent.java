package ma.locavite.reservation.event;

import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.Refund;

public record ReservationCancelledEvent(Reservation reservation, Refund refund, String avoirPath) {
}
