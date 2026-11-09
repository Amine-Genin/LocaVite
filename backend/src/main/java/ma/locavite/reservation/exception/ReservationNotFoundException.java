package ma.locavite.reservation.exception;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(Long id) {
        super("Réservation non trouvée avec l'id : " + id);
    }

    public ReservationNotFoundException(String message) {
        super(message);
    }
}
