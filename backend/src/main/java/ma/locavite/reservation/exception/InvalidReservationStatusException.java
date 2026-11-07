package ma.locavite.reservation.exception;

public class InvalidReservationStatusException extends RuntimeException {
    public InvalidReservationStatusException(String message) {
        super(message);
    }
}
