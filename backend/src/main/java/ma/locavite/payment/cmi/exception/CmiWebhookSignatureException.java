package ma.locavite.payment.cmi.exception;

public class CmiWebhookSignatureException extends CmiSignatureException {

    public CmiWebhookSignatureException(String message) {
        super(message);
    }
}
