package ma.locavite.payment.cmi.exception;

public class CmiIpNotAllowedException extends RuntimeException {
    public CmiIpNotAllowedException(String message) {
        super(message);
    }
}
