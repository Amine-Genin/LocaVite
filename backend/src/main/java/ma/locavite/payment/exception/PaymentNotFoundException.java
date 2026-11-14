package ma.locavite.payment.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long id) {
        super("Paiement non trouvé avec l'id : " + id);
    }

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
