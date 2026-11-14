package ma.locavite.payment.exception;

public class InvalidPaymentStatusException extends IllegalStateException {

    public InvalidPaymentStatusException(String message) {
        super(message);
    }
}
