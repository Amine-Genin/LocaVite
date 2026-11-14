package ma.locavite.payment.exception;

public class PaymentValidationException extends IllegalArgumentException {

    public PaymentValidationException(String message) {
        super(message);
    }
}
