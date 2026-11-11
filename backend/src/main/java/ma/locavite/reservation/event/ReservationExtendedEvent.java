package ma.locavite.reservation.event;

import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationContract;
import ma.locavite.reservation.domain.ReservationExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationExtendedEvent(
        Reservation reservation,
        ReservationExtension extension,
        ReservationContract contract
) {
    public Long reservationId() {
        return reservation != null ? reservation.getId() : (extension != null ? extension.getReservationId() : null);
    }

    public LocalDate ancienneDateFin() {
        return extension != null ? extension.getAncienneDateFin() : null;
    }

    public LocalDate nouvelleDateFin() {
        return extension != null ? extension.getNouvelleDateFin() : (reservation != null ? reservation.getDateFin() : null);
    }

    public BigDecimal prixSupplementaire() {
        return extension != null ? extension.getPrixSupplementaire() : null;
    }
}
