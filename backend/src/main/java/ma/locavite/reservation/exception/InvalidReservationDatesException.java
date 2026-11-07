package ma.locavite.reservation.exception;

public class InvalidReservationDatesException extends RuntimeException {
    public InvalidReservationDatesException(String message) {
        super(message);
    }
}
