package ma.locavite.reservation.exception;

public class SeasonOverlapException extends RuntimeException {
    public SeasonOverlapException(String message) {
        super(message);
    }
}
