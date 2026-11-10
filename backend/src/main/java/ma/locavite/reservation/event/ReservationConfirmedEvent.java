package ma.locavite.reservation.event;

import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationContract;

public record ReservationConfirmedEvent(Reservation reservation, ReservationContract contract) {}
