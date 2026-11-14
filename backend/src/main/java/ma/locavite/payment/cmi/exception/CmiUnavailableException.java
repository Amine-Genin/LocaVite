package ma.locavite.payment.cmi.exception;

public class CmiUnavailableException extends RuntimeException {
    public CmiUnavailableException(String message) {
        super(message);
    }

    public CmiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
