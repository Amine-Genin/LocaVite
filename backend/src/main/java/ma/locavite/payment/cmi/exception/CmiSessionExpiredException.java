package ma.locavite.payment.cmi.exception;

public class CmiSessionExpiredException extends RuntimeException {
    public CmiSessionExpiredException(String message) {
        super(message);
    }
}
