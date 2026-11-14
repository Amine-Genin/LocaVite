package ma.locavite.payment.domain;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    CANCELLED;

    public boolean canTransitionTo(PaymentStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == PROCESSING || target == SUCCESS || target == FAILED || target == CANCELLED;
            case PROCESSING -> target == SUCCESS || target == FAILED || target == CANCELLED;
            case SUCCESS -> target == REFUNDED || target == PARTIALLY_REFUNDED;
            case PARTIALLY_REFUNDED -> target == REFUNDED;
            case FAILED, REFUNDED, CANCELLED -> false;
        };
    }
}
