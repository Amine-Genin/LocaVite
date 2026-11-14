package ma.locavite.payment.cmi.exception;

public class CmiRateLimitExceededException extends RuntimeException {

    public CmiRateLimitExceededException(String message) {
        super(message);
    }
}
