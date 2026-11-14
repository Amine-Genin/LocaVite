package ma.locavite.payment.cmi.exception;

public class CmiSignatureException extends RuntimeException {
    public CmiSignatureException(String message) {
        super(message);
    }
}
